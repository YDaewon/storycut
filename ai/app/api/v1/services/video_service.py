import os
import httpx
from app.api.v1.services.springboot_service import get_video_from_springboot
from app.api.v1.services.mosaic_service import run_mosaic_pipeline
from app.api.v1.services.subtitle_service import subtitles
from app.api.v1.services.bgm_service import process_bgm_service  
from app.api.v1.services.video_analysis import run_analysis_pipeline
from app.api.v1.services.video_edit_service import select_time_ranges_by_prompt
async def download_video_to_local(videoUrl: str, save_path: str):
    async with httpx.AsyncClient() as client:
        response = await client.get(videoUrl)
        if response.status_code != 200:
            raise RuntimeError(f"영상 다운로드 실패: {response.status_code} - {response.text}")
        with open(save_path, "wb") as f:
            f.write(response.content)

async def process_video_job(
    prompt: str,
    video_id: int,
    images: list,
    subtitle: bool,
    music_prompt:str,
    token: str,
) -> str:

    video_info = await get_video_from_springboot(video_id, token)
    video_name = video_info.result.video_name
    video_path = os.path.join("app/videos", video_name)
    new_video_path = None
    is_blur = False

    if not os.path.isfile(video_path):
        print(f"{video_name} 다운로드 중...")
        await download_video_to_local(video_info.result.video_url, video_path)

    print("영상 분석 중...")
    if prompt:
        new_video_path = await select_time_ranges_by_prompt(video_path=video_path,user_prompt=prompt)
        os.remove(video_path)
        video_path = new_video_path

    if images:
        new_video_path = await run_mosaic_pipeline(video_path, images, 5, 3)
        os.remove(video_path)
        video_path = new_video_path
        is_blur = True

    if music_prompt:
        new_video_path = await process_bgm_service(video_path,prompt)
        os.remove(video_path)
        video_path = new_video_path

    if subtitle:
        new_video_path = await subtitles(video_path)
        video_path = new_video_path

    return video_path, is_blur
