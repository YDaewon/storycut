import os
import httpx
from app.api.v1.services.springboot_service import get_video_from_springboot
from app.api.v1.services.mosaic_service import run_mosaic_pipeline
from app.api.v1.services.subtitle_service import subtitles
from app.api.v1.services.bgm_service import process_bgm_service
from app.api.v1.services.video_analysis import run_analysis_pipeline
from app.api.v1.services.video_edit_service import select_time_ranges_by_prompt
from app.core.logger import logger


async def download_video_to_local(videoUrl: str, save_path: str):
    async with httpx.AsyncClient() as client:
        response = await client.get(videoUrl)
        if response.status_code != 200:
            raise RuntimeError(f"영상 다운로드 실패: {response.status_code} - {response.text}")
        with open(save_path, "wb") as f:
            f.write(response.content)
    logger.info(f"[Download] 영상 다운로드 완료 → {save_path}")


async def process_video_job(
    prompt: str,
    video_id: int,
    images: list,
    subtitle: bool,
    music_prompt: str,
    auto_music: bool,
    token: str,
) -> str:
    logger.info(f"[VideoJob] 영상 처리 시작 | video_id={video_id}")

    video_info = await get_video_from_springboot(video_id, token)
    video_name = os.path.basename(video_info.result.video_url)
    video_path = os.path.join("app/videos", video_name)
    new_video_path = None
    raw_data=None
    is_blur = False

    if not os.path.isfile(video_path):
        logger.info(f"[Download] 원본 영상 다운로드 중 → {video_name}")
        await download_video_to_local(video_info.result.video_url, video_path)
    else:
        logger.info(f"[Download] 로컬에 기존 영상 존재 → {video_name}")

    if prompt:
        logger.info("[CutEdit] 프롬프트 기반 분석 및 컷 편집 시작")
        new_video_path, raw_data = await select_time_ranges_by_prompt(video_path=video_path, user_prompt=prompt)
        os.remove(video_path)
        logger.info("[CutEdit] 컷 편집 완료 → 기존 영상 제거")
        video_path = new_video_path

    if subtitle:
        logger.info("[Subtitle] 자막 생성 시작")
        new_video_path = await subtitles(video_path)
        os.remove(video_path)
        logger.info("[Subtitle] 자막 삽입 완료 → 기존 영상 제거")
        video_path = new_video_path

    if music_prompt:
        logger.info("[BGM] 배경음악 생성 및 삽입 시작")
        new_video_path = await process_bgm_service(video_path, music_prompt)
        os.remove(video_path)
        logger.info("[BGM] BGM 삽입 완료 → 기존 영상 제거")
        video_path = new_video_path

    if auto_music:
        if not raw_data :
            raw_data=await run_analysis_pipeline(video_path)
        new_video_path = await process_bgm_service(video_path, raw_data)
        os.remove(video_path)
        logger.info("[BGM] BGM 삽입 완료 → 기존 영상 제거")
        video_path = new_video_path


    if images:
        logger.info("[Mosaic] 모자이크 처리 시작")
        new_video_path = await run_mosaic_pipeline(video_path, images, 5, 3)
        os.remove(video_path)
        logger.info("[Mosaic] 모자이크 처리 완료 → 기존 영상 제거")
        video_path = new_video_path
        is_blur = True

    logger.info(f"[VideoJob] 처리 완료 → 최종 경로: {video_path} | is_blur={is_blur}")
    return video_path, is_blur
