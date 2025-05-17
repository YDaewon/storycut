import os
from typing import List
from fastapi import APIRouter, UploadFile, Form, File, Header
from fastapi.responses import JSONResponse
from app.core.logger import logger

# 서비스 로직
from app.api.v1.services.upload_service import (
    save_uploaded_images,
    save_uploaded_video_local,
    save_uploaded_video,
    generate_and_upload_thumbnail,
    save_uploaded_image
)
from app.api.v1.services.springboot_service import (
    post_video_to_springboot_upload,
    post_video_to_springboot_complete
)

# 스키마
from app.api.v1.schemas.upload_schema import (
    ImageUploadResponse, ErrorResponse,
    ImageUploadResult, VideoUploadResponse,
    RoomThumbnailResponse, RoomThumbnailResult
)
from app.api.v1.schemas.post_schema import CompleteRequest, UploadRequest

router = APIRouter()


# 이미지 업로드
@router.post(
    "/images",
    response_model=ImageUploadResponse,
    responses={400: {"model": ErrorResponse, "description": "이미지 요청이 잘못되었습니다."}},
    summary="이미지 업로드"
)
async def upload_images(files: List[UploadFile] = File(...)):
    try:
        logger.info(f"[이미지 업로드 시작] 파일 수: {len(files)}")
        image_urls = await save_uploaded_images(files)
        logger.info(f"[이미지 업로드 완료] URL 수: {len(image_urls)}")

        return ImageUploadResponse(
            is_success=True,
            code=200,
            message="요청에 성공하였습니다.",
            result=ImageUploadResult(image_urls=image_urls)
        )
    except Exception:
        logger.exception("[이미지 업로드 실패]")
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                code=400,
                message="이미지 요청이 잘못되었습니다.",
                result=None
            ).dict(by_alias=True)
        )


# 영상 업로드
@router.post(
    "/videos",
    response_model=VideoUploadResponse,
    responses={400: {"model": ErrorResponse, "description": "영상 업로드 실패"}},
    summary="영상 업로드"
)
async def upload_video(
    file: UploadFile = File(...),
    video_title: str = Form(...),
    authorization: str = Header(..., alias="Authorization")
):
    token = authorization.replace("Bearer ", "")
    try:
        logger.info(f"[영상 업로드 시작] 제목: {video_title}, 파일명: {file.filename}")

        local_path = await save_uploaded_video_local(file)
        video_name = os.path.basename(local_path)
        logger.info(f"[로컬 저장 완료] 경로: {local_path}")

        thumbnail_url = await generate_and_upload_thumbnail(local_path)
        logger.info(f"[썸네일 생성 완료] URL: {thumbnail_url}")

        s3_url = await save_uploaded_video(local_path, video_name)
        logger.info(f"[S3 업로드 완료] URL: {s3_url}")

        payload = UploadRequest(
            video_title=video_title,
            original_video_id=None,
            is_blur=False,
        )

        id = await post_video_to_springboot_upload(token, payload)
        logger.info(f"[Spring 업로드 등록 완료] video_id: {id.result}")

        complete_payload = CompleteRequest(
            video_id=id.result,
            thumbnail=thumbnail_url,
            video_url=s3_url,
        )

        spring_response = await post_video_to_springboot_complete(token, complete_payload)
        logger.info(f"[Spring 완료 전송 성공] video_id: {id.result}")

        return VideoUploadResponse(
            is_success=True,
            code=200,
            message="영상 업로드 성공",
            result=spring_response.result
        )

    except Exception:
        logger.exception("[영상 업로드 실패]")
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                code=400,
                message="영상이 업로드가 잘못되었습니다.",
                result=None
            ).dict(by_alias=True)
        )


# 룸 썸네일 이미지 업로드
@router.post(
    "/room-thumbnails",
    response_model=RoomThumbnailResponse,
    responses={400: {"model": ErrorResponse, "description": "룸 썸네일 업로드 실패"}},
    summary="룸 썸네일 이미지 업로드"
)
async def upload_room_thumbnail(file: UploadFile = File(...)):
    try:
        logger.info(f"[룸 썸네일 업로드 시작] 파일명: {file.filename}")
        uploaded_url = await save_uploaded_image(file)
        logger.info(f"[룸 썸네일 업로드 완료] URL: {uploaded_url}")

        return RoomThumbnailResponse(
            is_success=True,
            code=200,
            message="룸 썸네일 업로드 성공",
            result=RoomThumbnailResult(url=uploaded_url)
        )
    except Exception:
        logger.exception("[룸 썸네일 업로드 실패]")
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                code=400,
                message="룸 썸네일 업로드 실패",
                result=None
            ).dict(by_alias=True)
        )
