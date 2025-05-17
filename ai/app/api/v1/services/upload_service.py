import os
import asyncio
import aiofiles
import aioboto3
import subprocess
import logging
from uuid import uuid4
from typing import List
from functools import partial
from fastapi import UploadFile
from app.core.config import settings
from app.core.logger import logger

# 디렉토리 생성
UPLOAD_DIR = "app/images"
VIDEO_DIR = "app/videos"
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(VIDEO_DIR, exist_ok=True)

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

            saved_urls.append(f"{UPLOAD_DIR}/{filename}")
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
    session = aioboto3.Session()
    try:
        logger.info("[S3 업로드] 시작")
        async with session.client("s3", region_name=settings.AWS_REGION) as s3_client:
            s3_key = f"videos/{filename}"
            async with aiofiles.open(local_path, "rb") as f:
                await s3_client.upload_fileobj(
                    f,
                    settings.S3_BUCKET_NAME,
                    s3_key,
                    ExtraArgs={"ContentType": "video/mp4"}
                )
        url = f"https://{settings.S3_BUCKET_NAME}.s3.{settings.AWS_REGION}.amazonaws.com/{s3_key}"
        logger.info("[S3 업로드] 완료: %s", url)
        return url
    except Exception:
        logger.exception("[S3 업로드] 실패")
        raise RuntimeError("S3 업로드 실패")

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

        session = aioboto3.Session()
        async with session.client("s3", region_name=settings.AWS_REGION) as s3_client:
            s3_key = f"thumbnails/{thumbnail_filename}"
            logger.info("[S3 업로드] 썸네일 시작: %s → %s", thumbnail_path, s3_key)
            async with aiofiles.open(thumbnail_path, "rb") as f:
                await s3_client.upload_fileobj(
                    f,
                    settings.S3_BUCKET_NAME,
                    s3_key,
                    ExtraArgs={"ContentType": "image/jpeg"}
                )
        url = f"https://{settings.S3_BUCKET_NAME}.s3.{settings.AWS_REGION}.amazonaws.com/{s3_key}"
        logger.info("[S3 업로드] 썸네일 완료: %s", url)
        return url

    except Exception:
        logger.exception("[썸네일 처리] 실패")
        raise RuntimeError("썸네일 처리 실패")

async def save_uploaded_image(file: UploadFile) -> str:
    session = aioboto3.Session()
    try:
        ext = file.filename.split('.')[-1]
        filename = f"{uuid4().hex}.{ext}"
        local_path = os.path.join(UPLOAD_DIR, filename)

        logger.info("[이미지 업로드] 로컬 저장 시작")
        async with aiofiles.open(local_path, "wb") as out_file:
            content = await file.read()
            await out_file.write(content)

        logger.info("[S3 업로드] 이미지 시작")
        async with session.client("s3", region_name=settings.AWS_REGION) as s3_client:
            s3_key = f"images/{filename}"
            async with aiofiles.open(local_path, "rb") as f:
                await s3_client.upload_fileobj(
                    f,
                    settings.S3_BUCKET_NAME,
                    s3_key,
                    ExtraArgs={"ContentType": file.content_type}
                )
        os.remove(local_path)
        url = f"https://{settings.S3_BUCKET_NAME}.s3.{settings.AWS_REGION}.amazonaws.com/{s3_key}"
        logger.info("[S3 업로드] 이미지 완료: %s", url)
        return url
    except Exception:
        logger.exception("[S3 업로드] 이미지 실패")
        raise RuntimeError("S3 이미지 업로드 실패")