import os
import asyncio
from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import JSONResponse

from app.api.v1.schemas.video_schema import VideoPostResponse, VideoProcessRequest
from app.api.v1.schemas.post_schema import UploadRequest, CompleteRequest, CompleteResponse
from app.api.v1.schemas.upload_schema import ErrorResponse

from app.api.v1.services.video_service import process_video_job
from app.api.v1.services.upload_service import generate_and_upload_thumbnail, save_uploaded_video
from app.api.v1.services.springboot_service import post_video_to_springboot_upload, post_video_to_springboot_complete
from app.core.fcm import send_result_fcm, send_failed_fcm
from app.core.logger import logger

router = APIRouter()

@router.post("", response_model=VideoPostResponse, summary="영상 요약 + 모자이크 + 자막 처리")
async def process_video(
    request: VideoProcessRequest,
    authorization: str = Header(...),
    device_token: str = Header(...)
):
    try:
        token = authorization.replace("Bearer ", "")
        logger.info(f"[{request.video_id}] 영상 업로드 요청 시작")

        payload = UploadRequest(
            video_title=request.video_title,
            original_video_id=request.video_id,
            is_blur=False,
        )

        response = await post_video_to_springboot_upload(token, payload)
        logger.info(f"[{request.video_id}] SpringBoot 업로드 등록 완료")

        # 백그라운드 비동기 작업 시작
        logger.info(f"[{request.video_id}] 백그라운드 영상 처리 시작")
        asyncio.create_task(process_video_pipeline(request, token, device_token, response.result))

        return VideoPostResponse(
            is_success=True,
            code=202,
            message="영상 처리 중입니다. 완료되면 알림이 전송됩니다.",
            result=None
        )

    except Exception:
        logger.exception(f"[{request.video_id}] 영상 업로드 요청 중 예외 발생")
        return JSONResponse(
            status_code=500,
            content=ErrorResponse(
                code=500,
                message="영상 업로드 요청 실패",
                result=None
            ).dict(by_alias=True)
        )


# 🎥 백그라운드 영상 처리 함수
async def process_video_pipeline(request: VideoProcessRequest, token: str, device_token: str, id: int):
    try:
        logger.info(f"[{request.video_id}] 영상 처리 파이프라인 시작")

        video_path, is_blur = await process_video_job(
            prompt=request.prompt,
            video_id=request.video_id,
            images=request.images,
            subtitle=request.subtitle,
            music_prompt=request.music_prompt,
            auto_music=request.auto_music,
            token=token
        )

        video_name = os.path.basename(video_path)
        logger.info(f"[{request.video_id}] 처리된 영상 저장 완료 - 경로: {video_path}")

        thumbnail_url = await generate_and_upload_thumbnail(video_path)
        logger.info(f"[{request.video_id}] 썸네일 생성 완료 - URL: {thumbnail_url}")

        s3_url = await save_uploaded_video(video_path, video_name)
        logger.info(f"[{request.video_id}] S3 업로드 완료 - URL: {s3_url}")

        payload = CompleteRequest(
            video_id=id,
            thumbnail=thumbnail_url,
            video_url=s3_url,
        )

        spring_response = await post_video_to_springboot_complete(token, payload)
        logger.info(f"[{request.video_id}] SpringBoot 완료 전송 완료")

        if spring_response.result:
            logger.info(f"[{request.video_id}] 최종 처리 성공 - FCM 발송")
            send_result_fcm(device_token, spring_response)
        else:
            logger.warning(f"[{request.video_id}] Spring 응답에 result 없음 - FCM 실패 처리")
            send_failed_fcm(device_token, code=500, message="영상 처리 결과가 없습니다.")

    except Exception:
        logger.exception(f"[{request.video_id}] 백그라운드 처리 중 예외 발생")
        send_failed_fcm(device_token, code=500, message="영상 처리 중 오류가 발생했습니다.")
