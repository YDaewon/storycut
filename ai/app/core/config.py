import os
from dotenv import load_dotenv

# 📌 .env 경로를 현재 파일 기준으로 상위에서 명확히 지정
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_PATH = os.path.join(BASE_DIR, ".env")
load_dotenv(dotenv_path=ENV_PATH)


class Settings:
    """
    🔧 .env 파일 기반 설정 클래스
    Azure Blob Storage 등 전역 환경 변수들을 관리
    """

    def __init__(self):
        # Azure Blob Storage 설정 (Connection String 없이)
        self.AZURE_STORAGE_ACCOUNT_NAME = os.getenv("AZURE_STORAGE_ACCOUNT_NAME")
        self.AZURE_STORAGE_ACCOUNT_KEY = os.getenv("AZURE_STORAGE_ACCOUNT_KEY")
        self.AZURE_CONTAINER_NAME = os.getenv("AZURE_CONTAINER_NAME", "my-container")

settings = Settings()
