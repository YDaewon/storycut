from pydantic import BaseModel, Field
from typing import Optional, List


# 🔹 업로드 요청용 스키마
class UploadRequest(BaseModel):
    video_title: str = Field(..., alias="videoTitle")
    original_video_id: Optional[int] = Field(None, alias="originalVideoId")
    is_blur: bool = Field(..., alias="isBlur")

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "videoTitle": "영상 제목",
                "originalVideoId": None,
                "isBlur": False
            }
        }


# 🔹 업로드 응답 스키마
class UploadResponse(BaseModel):
    is_success: bool = Field(..., alias="isSuccess")
    code: int
    message: str
    result: int  # 영상 ID

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "isSuccess": True,
                "code": 200,
                "message": "요청에 성공하였습니다.",
                "result": 12
            }
        }


# 🔹 영상 완료 처리 요청 스키마
class CompleteRequest(BaseModel):
    video_id: int = Field(..., alias="videoId")
    video_url: str = Field(..., alias="videoUrl")
    thumbnail: str = Field(..., alias="thumbnail")

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "videoId": 1,
                "videoUrl": "https://example.com/video.mp4",
                "thumbnail": "https://example.com/thumbnail.img"
            }
        }


# 🔹 영상 정보 결과 스키마
class CompleteResult(BaseModel):
    video_id: int = Field(..., alias="videoId")
    member_id: int = Field(..., alias="memberId")
    video_url: str = Field(..., alias="videoUrl")
    video_title: str = Field(..., alias="videoTitle")
    thumbnail: str
    original_video_id: Optional[int] = Field(None, alias="originalVideoId")
    created_at: str = Field(..., alias="createdAt")
    updated_at: str = Field(..., alias="updatedAt")
    is_blur: Optional[bool] = Field(None, alias="isBlur")
    class Config:
        allow_population_by_field_name = True
        orm_mode = True


# 🔹 완료 응답 스키마
class CompleteResponse(BaseModel):
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
                "result": 
                    {
                        "videoId": 1,
                        "memberId": 42,
                        "videoTitle": "example.mp4",
                        "videoUrl": "https://example.com/videos/example.mp4",
                        "thumbnail": "https://example.com/thumbnails/example.jpg",
                        "originalVideoId": None,
                        "createdAt": "2025-05-14T12:00:00",
                        "updatedAt": "2025-05-14T12:00:00",
                        "isBlur":True
                    }
            }
        }
