import os
import logging
import asyncio
from fastapi import APIRouter, Header, HTTPException
from app.api.v1.services.mosaic_service import run_mosaic_pipeline
from app.api.v1.services.springboot_service import (
    get_video_from_springboot,
    post_video_to_springboot_complete,
    post_video_to_springboot_upload,
)
from app.api.v1.services.upload_service import (
    generate_and_upload_thumbnail,
    save_uploaded_video,
)
from app.api.v1.schemas.mosaic_schema import MosaicRequest
from app.api.v1.schemas.video_schema import VideoPostResponse
from app.api.v1.schemas.post_schema import UploadRequest, CompleteRequest, CompleteResponse
from app.core.fcm import send_result_fcm, send_failed_fcm  # ✅
from app.api.v1.schemas.upload_schema import ErrorResponse
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

router = APIRouter()

@router.post("", response_model=VideoPostResponse, summary="모자이크")
async def process_video_from_json(
    request: MosaicRequest,
    authorization: str = Header(...),
    device_token: str = Header(...)
):
    try:
        token = authorization.replace("Bearer ", "")
        video_info = await get_video_from_springboot(request.video_id, token)

        if video_info.result.is_blur:
            return JSONResponse(
                status_code=400,
                content=ErrorResponse(code=400,message="이미 모자이크 처리된 영상입니다.",result=None).dict(by_alias=True)
            )

        payload = UploadRequest(
            videoTitle=request.video_title,
            original_video_id=request.video_id,
            is_blur=True
        )

        spring_response = await post_video_to_springboot_upload(token, payload)

        # 🎬 백그라운드 비동기 처리
        asyncio.create_task(
            process_video_pipeline(
                token,
                request,
                video_info.result.video_url,
                device_token,
                spring_response.result  # video_id
            )
        )

        return VideoPostResponse(
            is_success=True,
            code=202,
            message="영상 처리 중입니다. 완료되면 알림이 전송됩니다.",
            result=None
        )

    except Exception as e:
        logger.exception("예외 발생:")
        return JSONResponse(status_code=500,
        content=ErrorResponse(code=500,message=f"처리 중 오류 발생: {str(e)}",result=None).dict(by_alias=True)
        )

# ✅ 백그라운드 모자이크 처리 함수
async def process_video_pipeline(token: str, request: MosaicRequest, video_url: str, device_token: str, id: int):
    try:
        video_path = await run_mosaic_pipeline(
            input_path=video_url,
            target_paths=request.images[:2],
            detect_interval=5,
            num_segments=3
        )
        video_name = os.path.basename(video_path)
        logger.info(f"처리된 영상 경로: {video_path}")

        thumbnail_url = await generate_and_upload_thumbnail(video_path)
        logger.info(f"썸네일 URL: {thumbnail_url}")

        s3_url = await save_uploaded_video(video_path, video_name)
        logger.info(f"S3 업로드 완료: {s3_url}")

        payload = CompleteRequest(
            video_id=id,
            thumbnail=thumbnail_url,
            video_url=s3_url,
        )

        spring_response = await post_video_to_springboot_complete(token, payload)

        if spring_response.result:
            send_result_fcm(device_token, spring_response)
        else:
            send_failed_fcm(device_token, code=500, message="Spring 응답에 result가 없습니다.")

    except Exception as e:
        logger.exception(f"[백그라운드 처리 오류]: {str(e)}")
        send_failed_fcm(device_token, code=500, message="영상 처리 중 오류가 발생했습니다.", error=e)
