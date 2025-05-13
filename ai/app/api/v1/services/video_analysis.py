import os
import cv2
import numpy as np
from operator import itemgetter
from mmaction.apis import init_recognizer, inference_recognizer
import torch

# 모델 설정 (초기화는 글로벌로 하여 성능 최적화)
config_file = './src/mmaction2/configs/recognition/tsn/tsn_imagenet-pretrained-r50_8xb32-1x1x8-100e_kinetics400-rgb.py'
checkpoint_file = './src/mmaction2/checkpoints/tsn_imagenet-pretrained-r50_8xb32-1x1x8-100e_kinetics400-rgb_20220906-2692d16c.pth'
label_file = './src/mmaction2/tools/data/kinetics/label_map_k400.txt'

with open(label_file, 'r') as f:
    labels = [line.strip() for line in f]

async def run_analysis_pipeline(video_path: str) -> list:
    model = init_recognizer(config_file, checkpoint_file, device='cuda:0')

    cap = cv2.VideoCapture(video_path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    window_sec = 5
    window_size = int(window_sec * fps)

    # 임시 클립 저장 경로
    tmp_dir = 'temp_clips'
    os.makedirs(tmp_dir, exist_ok=True)
    time_results = []

    for start_frame in range(0, total_frames, window_size):
        end_frame = min(start_frame + window_size, total_frames)
        frames = []

        cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)
        for _ in range(start_frame, end_frame):
            ret, frame = cap.read()
            if not ret:
                break
            frames.append(frame)

        if len(frames) == 0:
            continue

        # 임시 클립 저장
        temp_video = os.path.join(tmp_dir, f'temp_{start_frame}.mp4')
        h, w, _ = frames[0].shape
        fourcc = cv2.VideoWriter_fourcc(*'mp4v')
        writer = cv2.VideoWriter(temp_video, fourcc, fps, (w, h))
        for f in frames:
            writer.write(f)
        writer.release()

        # 영상 분석 (추론)
        pred_result = inference_recognizer(model, temp_video)
        pred_scores = pred_result.pred_score.tolist()
        score_tuples = tuple(zip(range(len(pred_scores)), pred_scores))
        score_sorted = sorted(score_tuples, key=itemgetter(1), reverse=True)
        top3_label = score_sorted[:2]
        results = [(labels[k[0]], k[1]) for k in top3_label]

        # 시간 정보와 결과 저장
        time_sec = (start_frame / fps, end_frame / fps)
        time_results.append((time_sec, results))

        # 임시 파일 삭제
        os.remove(temp_video)

    cap.release()
    del model  # 모델 객체 삭제
    torch.cuda.empty_cache()  # GPU 메모리 해제
    return time_results