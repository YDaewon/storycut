import httpx
from app.api.v1.schemas.post_schema import PostRequest, PostResponse

SPRINGBOOT_API_URL = "https://k12d108.p.ssafy.io/api/v1/spring/api/video"  # 실제 주소로 교체
# SPRINGBOOT_API_URL = "http://localhost:8080/api/video"  # 실제 주소로 교체
async def post_video_to_springboot(
    token: str,
    payload: PostRequest
) -> PostResponse:
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    async with httpx.AsyncClient() as client:
        response = await client.post(
            SPRINGBOOT_API_URL,
            headers=headers,
            json=payload.dict(by_alias=True)
        )

    if response.status_code != 200:
        raise RuntimeError(f"Spring Boot API 실패: {response.status_code} - {response.text}")

    return PostResponse.parse_obj(response.json())


async def get_video_from_springboot(
    video_id: int,
    token: str
) -> PostResponse:
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{SPRINGBOOT_API_URL}/{video_id}",
            headers=headers
        )

    if response.status_code != 200:
        raise RuntimeError(f"Spring Boot API 실패: {response.status_code} - {response.text}")

    return PostResponse.parse_obj(response.json())
