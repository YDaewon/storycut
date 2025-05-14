import os
import firebase_admin
from firebase_admin import credentials, messaging
from app.api.v1.schemas.post_schema import PostResult

# ✅ 서비스 계정 JSON 경로
SERVICE_ACCOUNT_PATH = "app/firebase/firebase-service-account.json"

# ✅ Firebase Admin 초기화 (이미 되어있으면 패스)
try:
    firebase_admin.get_app()
except ValueError:
    cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
    firebase_admin.initialize_app(cred)

# ✅ FCM 전송 함수 (HTTP v1 방식)
def send_fcm_notification(token: str, title: str, body: str, data: dict = {}):
    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body
        ),
        token=token,
        data={k: str(v) for k, v in data.items()}
    )
    response = messaging.send(message)
    return response

# ✅ PostResult 기반 전송 헬퍼
def send_result_fcm(device_token: str, result: PostResult):
    send_fcm_notification(
        token=device_token,
        title="📹 영상 업로드 완료",
        body=f"'{result.video_name}' 영상이 성공적으로 업로드되었습니다.",
        data=result.dict(by_alias=True)
    )
