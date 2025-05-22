import os.path
from uuid import uuid4
from datetime import datetime, timedelta

from fastapi import APIRouter, Query
from azure.storage.blob import generate_blob_sas, BlobSasPermissions
from app.api.v1.schemas.presigned_url_schema import PresignedUrlResponse
from app.core.config import settings  # ✅ 환경 변수 설정 클래스 import

router = APIRouter()

# ✅ 환경 변수 접근
AZURE_STORAGE_ACCOUNT_NAME = settings.AZURE_STORAGE_ACCOUNT_NAME
AZURE_STORAGE_ACCOUNT_KEY = settings.AZURE_STORAGE_ACCOUNT_KEY
AZURE_CONTAINER_NAME = settings.AZURE_CONTAINER_NAME
AZURE_BLOB_BASE_URL = f"https://{AZURE_STORAGE_ACCOUNT_NAME}.blob.core.windows.net"


@router.get(
    "/presigned-url",
    response_model=PresignedUrlResponse,
    summary="Azure Blob Presigned URL 발급",
    description="원본 파일명을 기반으로 확장자를 추출하여 UUID 기반 파일명을 만들고, Azure Blob Storage 업로드용 SAS URL을 생성합니다."
)
async def get_presigned_url(original_filename: str = Query(...)):
    # 확장자 추출
    _, ext = os.path.splitext(original_filename)
    ext = ext.lower() or ".bin"

    # UUID 기반 blob 이름 생성
    blob_name = f"videos/{uuid4().hex}{ext}"

    # SAS 토큰 생성
    sas_token = generate_blob_sas(
        account_name=AZURE_STORAGE_ACCOUNT_NAME,
        account_key=AZURE_STORAGE_ACCOUNT_KEY,
        container_name=AZURE_CONTAINER_NAME,
        blob_name=blob_name,
        permission=BlobSasPermissions(write=True,read=True,create=True),
        expiry=datetime.utcnow() + timedelta(minutes=5)
    )

    # URL 구성
    upload_url = f"{AZURE_BLOB_BASE_URL}/{AZURE_CONTAINER_NAME}/{blob_name}?{sas_token}"
    video_url = f"{AZURE_BLOB_BASE_URL}/{AZURE_CONTAINER_NAME}/{blob_name}"

    return {
        "uploadUrl": upload_url,
        "videoUrl": video_url
    }
