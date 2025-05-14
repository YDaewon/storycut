import httpx
from app.api.v1.schemas.post_schema import (
    UploadRequest, UploadResponse,
    CompleteRequest, CompleteResponse
)

BASE_URL = "https://k12d108.p.ssafy.io/api/v1/spring"
# BASE_URL = "http://localhost:8080/api/v1/spring"

def get_auth_headers(token: str) -> dict:
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

async def post_video_to_springboot_upload(
    token: str,
    payload: UploadRequest
) -> UploadResponse:
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{BASE_URL}/video",
                headers=get_auth_headers(token),
                json=payload.dict(by_alias=True)
            )
        response.raise_for_status()
        return UploadResponse.parse_obj(response.json())
    except httpx.HTTPStatusError as e:
        raise RuntimeError(f"업로드 실패: {e.response.status_code} - {e.response.text}")
    except Exception as e:
        raise RuntimeError(f"업로드 중 알 수 없는 오류 발생: {str(e)}")

async def post_video_to_springboot_complete(
    token: str,
    payload: CompleteRequest
) -> CompleteResponse:
    try:
        async with httpx.AsyncClient() as client:
            response = await client.patch(
                f"{BASE_URL}/video/complete",
                headers=get_auth_headers(token),
                json=payload.dict(by_alias=True)
            )
        response.raise_for_status()
        return CompleteResponse.parse_obj(response.json())
    except httpx.HTTPStatusError as e:
        raise RuntimeError(f"완료 처리 실패: {e.response.status_code} - {e.response.text}")
    except Exception as e:
        raise RuntimeError(f"완료 처리 중 알 수 없는 오류 발생: {str(e)}")

async def get_video_from_springboot(
    video_id: int,
    token: str
) -> CompleteResponse:
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{BASE_URL}/video/{video_id}",
                headers=get_auth_headers(token)
            )
        response.raise_for_status()
        return CompleteResponse.parse_obj(response.json())
    except httpx.HTTPStatusError as e:
        raise RuntimeError(f"영상 정보 조회 실패: {e.response.status_code} - {e.response.text}")
    except Exception as e:
        raise RuntimeError(f"조회 중 알 수 없는 오류 발생: {str(e)}")
