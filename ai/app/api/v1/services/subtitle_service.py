import os
import subprocess
import whisper
import torch
import re
import cv2
import uuid
import aiofiles
import logging
from typing import Tuple
from app.core.logger import logger

base_dir = "app/videos"
os.makedirs(base_dir, exist_ok=True)

def get_video_resolution(video_path: str) -> Tuple[int, int]:
    cap = cv2.VideoCapture(video_path)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    cap.release()
    return width, height

def format_time_ass(seconds: float) -> str:
    h = int(seconds // 3600)
    m = int((seconds % 3600) // 60)
    s = int(seconds % 60)
    cs = int(round((seconds - int(seconds)) * 100))
    return f"{h:01}:{m:02}:{s:02}.{cs:02}"

def run_ffmpeg_command_sync(cmd: list):
    result = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if result.returncode != 0:
        raise RuntimeError(f"ffmpeg 명령어 실패: {' '.join(cmd)}")

def has_audio_stream(video_path: str) -> bool:
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "a", "-show_entries", "stream=index", "-of", "csv=p=0", video_path],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    return bool(result.stdout.strip())

async def subtitles(video_path: str) -> str:
    uid = uuid.uuid4().hex
    audio_path = os.path.join(base_dir, f"{uid}_temp_audio.wav")
    ass_path = os.path.join(base_dir, f"{uid}_subtitle.ass")
    ffmpeg_ass_path = ass_path.replace("\\", "/")
    output_path = os.path.join(base_dir, f"{uid}_subtitled.mp4").replace("\\", "/")

    whisper_model = None

    try:
        if not has_audio_stream(video_path):
            logger.info("[자막 처리] 오디오 트랙 없음 - 자막 생략")
            run_ffmpeg_command_sync([
                "ffmpeg", "-y", "-i", video_path, "-c", "copy", output_path
            ])
            return output_path

        if not torch.cuda.is_available():
            raise RuntimeError("CUDA 사용 불가. GPU 설정을 확인하세요.")

        logger.info("[자막 처리] Whisper 모델 로딩 중...")
        whisper_model = whisper.load_model("medium").to("cuda")

        logger.info("[자막 처리] 오디오 추출 중...")
        run_ffmpeg_command_sync([
            "ffmpeg", "-y", "-i", video_path,
            "-ar", "16000", "-ac", "1", "-c:a", "pcm_s16le", audio_path
        ])

        logger.info("[자막 처리] 음성 인식 시작...")
        result = whisper_model.transcribe(
            audio_path,
            language="ko",
            task="transcribe",
            compression_ratio_threshold=2.8,
            logprob_threshold=-1.0,
            no_speech_threshold=0.5
        )

        width, height = get_video_resolution(video_path)
        base_ref = max(width, height)
        fontsize = max(16, int(48 * base_ref / 1080))
        margin_v = max(10, int(30 * base_ref / 1080 * (2.2 if height > width else 1)))

        async with aiofiles.open(ass_path, "w", encoding="utf-8") as f:
            await f.write("[Script Info]\n")
            await f.write(f"PlayResX: {width}\nPlayResY: {height}\nScriptType: v4.00+\n\n")
            await f.write("[V4+ Styles]\n")
            await f.write("Format: Name, Fontname, Fontsize, PrimaryColour, BackColour, Bold, Italic, "
                          "Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, "
                          "Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n")
            await f.write(f"Style: Default,Noto Sans CJK KR,{fontsize},&H00FFFFFF,&H80000000,0,0,0,0,100,100,0,0," 
                          f"3,2,0,2,30,30,{margin_v},1\n\n")
            await f.write("[Events]\n")
            await f.write("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n")

            for seg in result["segments"]:
                text = seg["text"].strip().replace("\n", " ")
                if seg.get("no_speech_prob", 0.0) > 0.6: continue
                if seg.get("avg_logprob", -10.0) < -1.2: continue
                if text == "": continue
                if re.match(r"^\[\(?[^\]]+\)?\]$", text): continue

                start = format_time_ass(seg["start"])
                end = format_time_ass(seg["end"])
                await f.write(f"Dialogue: 0,{start},{end},Default,,0,0,0,,{text}\n")

        logger.info("[자막 처리] ffmpeg로 자막 삽입 중...")
        run_ffmpeg_command_sync([
            "ffmpeg", "-y", "-i", video_path,
            "-vf", f"ass={ffmpeg_ass_path}",
            "-c:v", "h264_nvenc", "-preset", "fast", "-b:v", "2M",
            "-c:a", "aac", "-b:a", "192k",
            output_path
        ])

        logger.info("[자막 처리] 완료: %s", output_path)
        return output_path

    finally:
        try:
            if whisper_model is not None:
                del whisper_model
                torch.cuda.empty_cache()
                logger.info("[자막 처리] Whisper 모델 메모리 해제 완료")
        except Exception:
            logger.warning("[자막 처리] 모델 해제 중 예외 발생")

        for path in [audio_path, ass_path]:
            try:
                if os.path.exists(path):
                    os.remove(path)
                    logger.info("[자막 처리] 임시 파일 삭제 완료: %s", path)
            except Exception:
                logger.warning("[자막 처리] 임시 파일 삭제 중 예외 발생: %s", path)
