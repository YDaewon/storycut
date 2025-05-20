from pydantic import BaseModel, Field

class PresignedUrlResponse(BaseModel):
    upload_url: str = Field(..., alias="uploadUrl", example="https://your.blob.core.windows.net/container/videos/abc123.mp4?sv=...")
    video_url: str = Field(..., alias="videoUrl", example="https://your.blob.core.windows.net/container/videos/abc123.mp4")

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "uploadUrl": "https://your.blob.core.windows.net/container/videos/abc123.mp4?sv=...",
                "videoUrl": "https://your.blob.core.windows.net/container/videos/abc123.mp4"
            }
        }
