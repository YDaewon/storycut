import os
import mimetypes
import asyncio
import aiofiles
import subprocess
import logging
from uuid import uuid4
from typing import List
from functools import partial
from fastapi import UploadFile
from azure.storage.blob.aio import BlobServiceClient
from azure.storage.blob import ContentSettings
from app.core.config import settings
from app.core.logger import logger

# 디렉토리 생성
UPLOAD_DIR = "app/images"
VIDEO_DIR = "app/videos"
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(VIDEO_DIR, exist_ok=True)

# Azure Blob Client 생성
def get_blob_client(container: str, blob_name: str):
    account_url = f"https://{settings.AZURE_STORAGE_ACCOUNT_NAME}.blob.core.windows.net"
    credential = settings.AZURE_STORAGE_ACCOUNT_KEY
    service_client = BlobServiceClient(account_url=account_url, credential=credential)
    return service_client.get_blob_client(container=container, blob=blob_name)

async def save_uploaded_images(files: List[UploadFile]) -> List[str]:
    saved_urls = []
    try:
        for file in files:
            ext = file.filename.split(".")[-1]
            filename = f"{uuid4().hex}.{ext}"
            file_path = os.path.join(UPLOAD_DIR, filename)
            logger.info("[이미지 업로드] 저장 경로: %s", file_path)

            async with aiofiles.open(file_path, "wb") as buffer:
                content = await file.read()
                await buffer.write(content)

            mime_type = mimetypes.guess_type(filename)[0] or "application/octet-stream"
            blob_client = get_blob_client(settings.AZURE_CONTAINER_NAME, f"images/{filename}")
            async with aiofiles.open(file_path, "rb") as f:
                await blob_client.upload_blob(
                    await f.read(),
                    overwrite=True,
                    content_settings=ContentSettings(content_type=mime_type)
                )

            saved_urls.append(blob_client.url)
        return saved_urls
    except Exception:
        logger.exception("[이미지 업로드] 저장 중 예외 발생")
        raise RuntimeError("이미지 저장 중 오류 발생")

async def save_uploaded_video_local(file: UploadFile) -> str:
    try:
        ext = file.filename.split(".")[-1]
        filename = f"video_{uuid4().hex}.{ext}"
        file_path = os.path.join(VIDEO_DIR, filename)
        logger.info("[비디오 업로드] 로컬 저장 경로: %s", file_path)

        async with aiofiles.open(file_path, "wb") as buffer:
            content = await file.read()
            await buffer.write(content)

        return file_path
    except Exception:
        logger.exception("[비디오 업로드] 로컬 저장 실패")
        raise RuntimeError("로컬 영상 저장 실패")

async def save_uploaded_video(local_path: str, filename: str) -> str:
    try:
        logger.info("[Azure 업로드] 비디오 업로드 시작")
        mime_type = mimetypes.guess_type(filename)[0] or "video/mp4"
        blob_client = get_blob_client(settings.AZURE_CONTAINER_NAME, f"videos/{filename}")
        async with aiofiles.open(local_path, "rb") as f:
            await blob_client.upload_blob(
                await f.read(),
                overwrite=True,
                content_settings=ContentSettings(content_type=mime_type)
            )
        url = blob_client.url
        logger.info("[Azure 업로드] 완료: %s", url)
        return url
    except Exception:
        logger.exception("[Azure 업로드] 실패")
        raise RuntimeError("Azure 업로드 실패")

def generate_thumbnail_sync(video_path: str, thumbnail_path: str):
    command = [
        "ffmpeg", "-y", "-i", video_path,
        "-ss", "00:00:01.000", "-vframes", "1",
        thumbnail_path
    ]
    logger.info("[썸네일 생성] ffmpeg 명령 실행 시작")
    result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

    if result.returncode != 0:
        logger.error("[썸네일 생성] ffmpeg 실패 로그:\n%s", result.stderr.decode())
        raise RuntimeError("ffmpeg 썸네일 생성 실패")

async def generate_and_upload_thumbnail(video_path: str) -> str:
    thumbnail_filename = f"thumb_{uuid4().hex}.jpg"
    thumbnail_path = os.path.join(UPLOAD_DIR, thumbnail_filename)

    try:
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, partial(generate_thumbnail_sync, video_path, thumbnail_path))
        logger.info("[썸네일 생성] 완료: %s", thumbnail_path)

        mime_type = mimetypes.guess_type(thumbnail_filename)[0] or "image/jpeg"
        blob_client = get_blob_client(settings.AZURE_CONTAINER_NAME, f"thumbnails/{thumbnail_filename}")
        async with aiofiles.open(thumbnail_path, "rb") as f:
            await blob_client.upload_blob(
                await f.read(),
                overwrite=True,
                content_settings=ContentSettings(content_type=mime_type)
            )
        url = blob_client.url
        logger.info("[Azure 업로드] 썸네일 완료: %s", url)
        return url

    except Exception:
        logger.exception("[썸네일 처리] 실패")
        raise RuntimeError("썸네일 처리 실패")

async def save_uploaded_image(file: UploadFile) -> str:
    try:
        ext = file.filename.split('.')[-1]
        filename = f"{uuid4().hex}.{ext}"
        local_path = os.path.join(UPLOAD_DIR, filename)

        logger.info("[이미지 업로드] 로컬 저장 시작")
        async with aiofiles.open(local_path, "wb") as out_file:
            content = await file.read()
            await out_file.write(content)

        mime_type = mimetypes.guess_type(filename)[0] or "application/octet-stream"
        logger.info("[Azure 업로드] 이미지 시작")
        blob_client = get_blob_client(settings.AZURE_CONTAINER_NAME, f"images/{filename}")
        async with aiofiles.open(local_path, "rb") as f:
            await blob_client.upload_blob(
                await f.read(),
                overwrite=True,
                content_settings=ContentSettings(content_type=mime_type)
            )
        os.remove(local_path)
        url = blob_client.url
        logger.info("[Azure 업로드] 이미지 완료: %s", url)
        return url
    except Exception:
        logger.exception("[Azure 업로드] 이미지 실패")
        raise RuntimeError("Azure 이미지 업로드 실패")
