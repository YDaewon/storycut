## ✨ MMAction2 가운하게 설치하기 (Python 3.8 + CUDA 11.8)

### ✅ 1. 아나코다 가상환경(시간 버전 Python 3.8)으로 만들기

```bash
conda create -n mmaction2_env python=3.8 -y
```

### ✅ 2. 가상환경 키기

```bash
conda activate mmaction2_env
```

---

### ✅ 3. PyTorch (CUDA 11.8) 설치

```bash
pip install torch==2.1.0 torchvision==0.16.0 torchaudio==2.1.0 --index-url https://download.pytorch.org/whl/cu118
```

---

### ✅ 4. MMCV 설치 (CUDA 11.8 + torch 2.1.0)

```bash
pip install mmcv==2.1.0 -f https://download.openmmlab.com/mmcv/dist/cu118/torch2.1/index.html
```

---

### ✅ 5. 특정 requirements_Anaconda.txt 파일의 필요 패키지 일괄 설치

```bash
pip install -r requirements_Anaconda.txt
```



---

### ✅ 6. FastAPI 실행

```bash
uvicorn app.main:app --reload
```

> 파일 구조를 검토하여 `app.main:app`이 맞는지 확인해주세요. (`main.py` 안에 `app = FastAPI()` 있어야 합니다)

---

### ✅ 7. 가상환경 종료

```bash
conda deactivate
```

---

