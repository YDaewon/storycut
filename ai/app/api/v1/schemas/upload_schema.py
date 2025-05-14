
from pydantic import BaseModel, Field
from typing import List, Optional
from app.api.v1.schemas.post_schema import CompleteResult
class ImageUploadResult(BaseModel):
    image_urls: List[str] = Field(..., alias="imageUrls")

    class Config:
        allow_population_by_field_name = True


class ImageUploadResponse(BaseModel):
    is_success: bool = Field(..., alias="isSuccess")
    code: int
    message: str
    result: Optional[ImageUploadResult]

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "isSuccess": True,
                "code": 200,
                "message": "이미지 업로드 성공",
                "result": {
                    "imageUrls": [
                        "app/images/db6ae97fc8e4443b92bdb782162e474f.png"
                    ]
                }
            }
        }


class VideoUploadResponse(BaseModel):
    is_success: bool = Field(..., alias="isSuccess")
    code: int
    message: str
    result: Optional[CompleteResult]

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "isSuccess": True,
                "code": 200,
                "message": "요청에 성공하였습니다.",
                "result": {
                    "videoId": 1,
                    "memberId": 1,
                    "videoTile": "영상 제목",
                    "videoUrl": "https://example.com/video.mp4",
                    "thumbnail": "https://example.com/thumbnail.jpg",
                    "originalVideoId": None,
                    "createdAt": "2025-05-02T08:54:00.000Z",
                    "updatedAt": "2025-05-02T08:54:00.000Z",
                    "isBlur":True
                }
            }
        }


class ErrorResponse(BaseModel):
    is_success: bool = Field(default=False, alias="isSuccess", example=False)
    code: int = Field(..., example=400)
    message: str = Field(..., example="에러 메시지")
    result: Optional[None] = Field(default=None, example=None)

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "isSuccess": False,
                "code": 400,
                "message": "에러 메시지",
                "result": None
            }
        }


class RoomThumbnailResult(BaseModel):
    url: str = Field(..., example="https://your-bucket.s3.ap-northeast-2.amazonaws.com/images/thumb_123abc.jpg")

    class Config:
        allow_population_by_field_name = True


class RoomThumbnailResponse(BaseModel):
    is_success: bool = Field(..., alias="isSuccess", example=True)
    code: int = Field(..., example=200)
    message: str = Field(..., example="룸 썸네일 업로드 성공")
    result: Optional[RoomThumbnailResult]

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "isSuccess": True,
                "code": 200,
                "message": "룸 썸네일 업로드 성공",
                "result": {
                    "url": "https://your-bucket.s3.ap-northeast-2.amazonaws.com/images/thumb_123abc.jpg"
                }
            }
        }