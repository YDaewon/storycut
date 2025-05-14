import os
from fastapi import APIRouter, UploadFile, Form, File, Header
from fastapi.responses import JSONResponse
from typing import List

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


# ✅ 이미지 업로드
@router.post(
    "/images",
    response_model=ImageUploadResponse,
    responses={400: {"model": ErrorResponse, "description": "이미지 요청이 잘못되었습니다."}},
    summary="이미지 업로드"
)
async def upload_images(files: List[UploadFile] = File(...)):
    try:
        image_urls = await save_uploaded_images(files)
        return ImageUploadResponse(
            is_success=True,
            code=200,
            message="요청에 성공하였습니다.",
            result=ImageUploadResult(image_urls=image_urls)
        )
    except Exception as e:
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                code=400,
                message=f"이미지 요청이 잘못되었습니다. {str(e)}",
                result=None
            ).dict(by_alias=True)
        )


# ✅ 영상 업로드
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
        local_path = await save_uploaded_video_local(file)
        video_name = os.path.basename(local_path)

        thumbnail_url = await generate_and_upload_thumbnail(local_path)
        s3_url = await save_uploaded_video(local_path, video_name)

        payload = UploadRequest(
            video_title=video_title,
            original_video_id=None,
            is_blur=False,
        )

        id = await post_video_to_springboot_upload(token, payload)

        payload = CompleteRequest(
            video_id=id.result,
            thumbnail=thumbnail_url,
            video_url=s3_url,
        )

        spring_response = await post_video_to_springboot_complete(token, payload)

        return VideoUploadResponse(
            is_success=True,
            code=200,
            message="영상 업로드 성공",
            result=spring_response.result
        )

    except Exception as e:
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                code=400,
                message=f"영상이 업로드가 잘못되었습니다. ({str(e)})",
                result=None
            ).dict(by_alias=True)
        )


# ✅ 룸 썸네일 이미지 업로드
@router.post(
    "/room-thumbnails",
    response_model=RoomThumbnailResponse,
    responses={400: {"model": ErrorResponse, "description": "룸 썸네일 업로드 실패"}},
    summary="룸 썸네일 이미지 업로드"
)
async def upload_room_thumbnail(file: UploadFile = File(...)):
    try:
        uploaded_url = await save_uploaded_image(file)
        return RoomThumbnailResponse(
            is_success=True,
            code=200,
            message="룸 썸네일 업로드 성공",
            result=RoomThumbnailResult(url=uploaded_url)
        )
    except Exception as e:
        return JSONResponse(
            status_code=400,
            content=ErrorResponse(
                code=400,
                message=f"룸 썸네일 업로드 실패: {str(e)}",
                result=None
            ).dict(by_alias=True)
        )
