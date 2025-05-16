import os
import json
import ast
import uuid
import re
import requests
import subprocess
import logging
from typing import List, Tuple
from dotenv import load_dotenv
from app.api.v1.services.video_analysis import run_analysis_pipeline

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 환경변수에서 Gemini API Key 로드

API_KEY = os.getenv("GEMINI_API_KEY")
if not API_KEY:
    raise RuntimeError("GEMINI_API_KEY가 .env에 정의되어 있지 않습니다.")

# Gemini API 기본 정보
API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent"
HEADERS = {"Content-Type": "application/json"}


# Gemini 응답에서 코드블럭(```python ... ```) 태그 제거
def clean_gemini_code_block(response_text: str) -> str:
    return re.sub(r"^```(?:python)?\s*|\s*```$", "", response_text.strip(), flags=re.IGNORECASE | re.MULTILINE).strip()


# 원본 비디오의 전체 비트레이트를 추출
# 실패 시 기본값을 반환함 (영상 3Mbps, 오디오 128kbps)
def get_video_bitrate(video_path: str) -> Tuple[str, str]:
    def probe(stream: str) -> str:
        try:
            cmd = [
                "ffprobe", "-v", "error", "-select_streams", stream,
                "-show_entries", "format=bit_rate",
                "-of", "default=noprint_wrappers=1:nokey=1", video_path
            ]
            return subprocess.check_output(cmd).decode().strip()
        except:
            return ""

    video_bps = probe("v:0") or "3000000"  # 비디오 스트림이 없으면 3Mbps 사용
    audio_bps = probe("a:0") or "128000"   # 오디오 스트림이 없으면 128kbps 사용
    return video_bps, audio_bps


# 주 함수: 분석 결과와 프롬프트를 이용해 특정 구간을 추출하여 비디오를 자르고 병합함
async def select_time_ranges_by_prompt(
    video_path: str,
    user_prompt: str,
    threshold: float = 0.7
) -> str:
    logger.info(f"분석 시작: {video_path}, threshold={threshold}")
    filtered_data = []
    raw_data = await run_analysis_pipeline(video_path)
    logger.info(f"분석 완료, 총 {len(raw_data)}개 구간")

    for (start, end), actions in raw_data:
        high_confidence = [(label, round(score, 3)) for label, score in actions if score >= threshold]
        if high_confidence:
            filtered_data.append(((start, end), high_confidence))

    if not filtered_data:
        logger.info("기준 이상의 구간 없음 → 원본 복사")
        output_dir = "app/videos"
        os.makedirs(output_dir, exist_ok=True)
        output_path = os.path.join(output_dir, f"{uuid.uuid4().hex}.mp4")

        cmd = [
            "ffmpeg", "-y", "-i", video_path,
            "-c", "copy", "-movflags", "+faststart",
            output_path
        ]
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        logger.info(f"복사 완료 → {output_path}")
        return output_path

    try:
        logger.info("Gemini에 프롬프트 전송 중")
        response_text = gemini_translate_ko_to_en(user_prompt, filtered_data)
        cleaned_text = clean_gemini_code_block(response_text)
        time_ranges = ast.literal_eval(cleaned_text)
        logger.info(f"Gemini 응답 시간대 수: {len(time_ranges)}")

        if not isinstance(time_ranges, list):
            raise ValueError("Gemini 응답이 리스트 형식이 아님")

        output_path = fast_cut_and_merge_ffmpeg(video_path, time_ranges)
        logger.info(f"최종 병합 완료 → {output_path}")
        return output_path

    except Exception as e:
        logger.error(f"Gemini 처리 실패: {str(e)}")
        raise RuntimeError(f"Gemini 호출 실패: {str(e)}\n응답:\n{response_text if 'response_text' in locals() else '없음'}")


# Gemini에 요청을 보내 필터링된 행동 정보와 프롬프트를 기반으로 관심 시간대를 추출하도록 함
def gemini_translate_ko_to_en(
    prompt_ko: str,
    filtered_data: List[Tuple[Tuple[float, float], List[Tuple[str, float]]]]
) -> str:
    formatted_data = "\n".join([f"{start:.1f}~{end:.1f}: {acts}" for (start, end), acts in filtered_data])
    full_prompt = (
        f'사용자 프롬프트: "{prompt_ko}"\n\n'
        "예측된 시간대별 행동 목록입니다.\n"
        f"{formatted_data}\n\n"
        "프롬프트와 관련된 시간대를 선택하여 파이썬 튜플 리스트 형식으로 반환해주세요.\n"
        "단, 서로 연속되는 시간대는 하나로 병합하여 출력해주세요.\n"
        "코드나 설명 없이 결과 리스트만 출력해주세요.\n"
        '형식 예: [(0.0, 15.0), (25.0, 30.0)]'
    )

    data = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": full_prompt}]
            }
        ]
    }

    response = requests.post(f"{API_URL}?key={API_KEY}", headers=HEADERS, data=json.dumps(data))
    if response.status_code != 200:
        raise RuntimeError(f"Gemini API 오류: {response.status_code}, {response.text}")

    return response.json()["candidates"][0]["content"]["parts"][0]["text"].strip()


# FFmpeg를 이용해 지정된 시간대를 잘라서 각각 임시 파일로 저장한 뒤, 이를 병합하여 최종 영상을 생성
def fast_cut_and_merge_ffmpeg(video_path: str, time_ranges: List[Tuple[float, float]]) -> str:
    output_dir = "app/videos"
    os.makedirs(output_dir, exist_ok=True)
    temp_list_path = os.path.join(output_dir, "cut_list.txt")
    temp_files = []

    logger.info(f"자르기 시작: {len(time_ranges)}개 구간")

    video_bps, audio_bps = get_video_bitrate(video_path)
    video_bitrate = f"{int(int(video_bps) * 0.9)}"
    audio_bitrate = f"{int(int(audio_bps) * 0.9)}"

    for idx, (start, end) in enumerate(time_ranges):
        temp_name = os.path.join(output_dir, f"temp_{idx}.mp4")
        duration = end - start
        cmd = [
            "ffmpeg", "-y",
            "-i", video_path,
            "-ss", str(start), "-t", str(duration),
            "-avoid_negative_ts", "1", "-reset_timestamps", "1",
            "-c:v", "h264_nvenc", "-b:v", video_bitrate, "-preset", "fast",
            "-c:a", "aac", "-b:a", audio_bitrate,
            "-r", "30", "-movflags", "+faststart",
            temp_name
        ]
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        logger.info(f"구간 {idx+1}: {start}~{end}s → {temp_name}")
        temp_files.append(temp_name)

    with open(temp_list_path, "w") as f:
        for temp in temp_files:
            f.write(f"file '{os.path.abspath(temp)}'\n")

    final_name = f"{uuid.uuid4().hex}.mp4"
    output_path = os.path.join(output_dir, final_name)

    cmd_merge = [
        "ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", temp_list_path,
        "-c:v", "h264_nvenc", "-b:v", video_bitrate,
        "-c:a", "aac", "-b:a", audio_bitrate,
        "-r", "30", "-movflags", "+faststart",
        output_path
    ]
    subprocess.run(cmd_merge, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    logger.info(f"병합 완료: {output_path}")

    for f in temp_files:
        os.remove(f)
    os.remove(temp_list_path)

    return output_path
