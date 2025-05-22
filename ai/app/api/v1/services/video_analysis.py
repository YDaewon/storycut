import os
import cv2
import numpy as np
import torch
from operator import itemgetter
from mmaction.apis import init_recognizer, inference_recognizer
from app.core.logger import logger
import asyncio
from concurrent.futures import ThreadPoolExecutor

# 설정
config_file = './src/mmaction2/configs/recognition/tsn/tsn_imagenet-pretrained-r50_8xb32-1x1x8-100e_kinetics400-rgb.py'
checkpoint_file = './src/mmaction2/checkpoints/tsn_imagenet-pretrained-r50_8xb32-1x1x8-100e_kinetics400-rgb_20220906-2692d16c.pth'
label_file = './src/mmaction2/tools/data/kinetics/label_map_k400.txt'
tmp_dir = 'temp_clips'
os.makedirs(tmp_dir, exist_ok=True)

# 라벨 로딩
with open(label_file, 'r') as f:
    labels = [line.strip() for line in f]

# 스레드 풀 생성
executor = ThreadPoolExecutor(max_workers=2)  # GPU 경합 방지를 위해 제한

# 클립 분석 (동기 함수)
def _analyze_clip(model, temp_video, start_frame, end_frame, fps):
    try:
        pred_result = inference_recognizer(model, temp_video)
        pred_scores = pred_result.pred_score.tolist()
        score_tuples = tuple(zip(range(len(pred_scores)), pred_scores))
        score_sorted = sorted(score_tuples, key=itemgetter(1), reverse=True)
        top2_label = score_sorted[:2]
        results = [(labels[k[0]], k[1]) for k in top2_label]
        time_sec = (start_frame / fps, end_frame / fps)
        return (time_sec, results)
    except Exception as e:
        logger.error(f"[Analysis] 클립 분석 오류 ({temp_video}): {str(e)}")
        return None

# 비동기 래퍼
async def _analyze_clip_async(model, temp_video, start_frame, end_frame, fps):
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        executor, _analyze_clip, model, temp_video, start_frame, end_frame, fps
    )

# 메인 파이프라인
async def run_analysis_pipeline(video_path: str) -> list:
    logger.info(f"[Analysis] 영상 분석 시작 → {video_path}")

    try:
        model = init_recognizer(config_file, checkpoint_file, device='cuda:0')
        logger.info("[Analysis] 모델 로딩 완료")

        cap = cv2.VideoCapture(video_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        logger.info(f"[Analysis] FPS: {fps}, 총 프레임 수: {total_frames}")

        window_sec = 3
        window_size = int(window_sec * fps)
        results = []

        for start_frame in range(0, total_frames, window_size):
            end_frame = min(start_frame + window_size, total_frames)
            frames = []

            cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)
            for _ in range(start_frame, end_frame):
                ret, frame = cap.read()
                if not ret:
                    break
                frames.append(frame)

            if not frames:
                logger.warning(f"[Analysis] 클립 프레임 없음 → 구간: {start_frame}~{end_frame}")
                continue

            temp_video = os.path.join(tmp_dir, f'temp_{start_frame}.mp4')
            h, w, _ = frames[0].shape
            writer = cv2.VideoWriter(temp_video, cv2.VideoWriter_fourcc(*'mp4v'), fps, (w, h))
            for f in frames:
                writer.write(f)
            writer.release()
            logger.info(f"[Analysis] 임시 클립 저장 완료 → {temp_video}")

            # 🔥 분석 후 결과 바로 저장 (비동기 실행)
            result = await _analyze_clip_async(model, temp_video, start_frame, end_frame, fps)
            if result:
                results.append(result)

            # 클립 삭제
            os.remove(temp_video)
            logger.info(f"[Analysis] 임시 클립 삭제 완료 → {temp_video}")

        cap.release()
        del model
        torch.cuda.empty_cache()
        logger.info("[Analysis] 모델 해제 및 GPU 메모리 정리 완료")

        return results

    except Exception as e:
        logger.error(f"[Analysis] 오류 발생: {str(e)}")
        raise
