import os
import json
import logging
import firebase_admin
from firebase_admin import credentials, messaging
from firebase_admin.exceptions import FirebaseError
from fastapi import HTTPException
from app.api.v1.schemas.post_schema import CompleteResponse, CompleteResult
from app.core.logger import logger

# 서비스 계정 JSON 경로
SERVICE_ACCOUNT_PATH = os.getenv("SERVICE_ACCOUNT_PATH")

# Firebase Admin 초기화 (이미 되어있으면 패스)
try:
    firebase_admin.get_app()
except ValueError:
    cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
    firebase_admin.initialize_app(cred)

#  FCM 전송 함수
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
    logger.info(f"FCM 전송 성공: {title} - {body}")
    return response

#  영상 처리 성공 시 전송 (result는 JSON 문자열로 감싸 전송)
def send_result_fcm(device_token: str, response: CompleteResponse):
    if response.result is None:
        logger.error("FCM 전송 실패: result가 None입니다.")
        raise ValueError("result가 None이면 FCM 전송이 불가능합니다.")

    try:
        data = {
            "isSuccess": str(response.is_success),
            "code": str(response.code),
            "message": response.message,
            "result": json.dumps(response.result.dict(by_alias=True))
        }

        send_fcm_notification(
            token=device_token,
            title="📹 영상 업로드 완료",
            body="영상 처리가 완료되었습니다.",
            data=data
        )
    except FirebaseError as e:
        logger.exception("Firebase 오류 발생 중")
        send_failed_fcm(device_token, code=500, message="영상 처리 중 Firebase 오류 발생", error=e)
        raise HTTPException(status_code=500, detail=f"FCM 전송 실패: {str(e)}")
    except Exception as e:
        logger.exception("알 수 없는 예외 발생")
        send_failed_fcm(device_token, code=500, message="영상 처리 중 알 수 없는 오류 발생", error=e)
        raise HTTPException(status_code=500, detail=f"예상치 못한 오류: {str(e)}")

#  영상 처리 실패 시 전송 (오류 메시지 포함)
def send_failed_fcm(device_token: str, code: int, message: str, error: Exception = None):
    full_message = message
    if error:
        full_message += f" ({str(error)})"

    data = {
        "isSuccess": "false",
        "code": str(code),
        "message": full_message
    }

    try:
        send_fcm_notification(
            token=device_token,
            title="❌ 영상 처리 실패",
            body=full_message,
            data=data
        )
    except Exception as e:
        logger.error(f"[FCM 실패 메시지 전송 실패] {str(e)}")