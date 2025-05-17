# app/api/v1/endpoints/video_test.py

from fastapi import APIRouter,Header,HTTPException
from app.api.v1.services.video_analysis import run_analysis_pipeline
from app.core.fcm import send_fcm_notification
from app.api.v1.services.bgm_service import process_bgm_service
import os

router = APIRouter()

@router.get("/test_video_analysis")
async def test_video_analysis():
    # 테스트할 로컬 영상 경로 지정
    video_path = "app/videos/458f7a8f51b547028ddc1a9b4535881d.mp4"  # 여기 경로를 로컬의 실제 영상 파일로 변경

    if not os.path.isfile(video_path):
        return {"error": "영상 파일이 존재하지 않습니다.", "path": video_path}

    print("영상 분석 시작...")
    results = await run_analysis_pipeline(video_path)
    print("영상 분석 완료.")

    # 서버 로그 확인을 위해 결과 출력
    for (start, end), preds in results:
        print(f"{start:.1f}-{end:.1f}s:")
        for label, score in preds:
            print(f"  - {label:<25} {score:.4f}")
    results = await run_analysis_pipeline(video_path)
    
    return {"message": "영상 분석이 완료되었습니다.", "results": results}


@router.post("/test_fcm")
async def test_fcm():
    try:
        video_path = "C:/Users/SSAFY/Desktop/cs/app/uploads/subtitled.mp4" 
        raw_data=await run_analysis_pipeline(video_path)
        new_video_path = await process_bgm_service(video_path, raw_data)

        return {"message": new_video_path}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"FCM 전송 실패: {str(e)}")
