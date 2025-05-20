from fastapi import FastAPI
from app.api.v1.endpoints import upload, video, mosaic, video_test,presigned
from fastapi.staticfiles import StaticFiles
from fastapi.openapi.utils import get_openapi

app = FastAPI(
    root_path="/api/v1/fastapi",      # Nginx proxy 경로와 일치
    docs_url="/docs",                 # Swagger UI 경로
    redoc_url=None,                   # Redoc 비활성화
    openapi_url="/openapi.json"       # OpenAPI JSON 경로
)

# ✅ Bearer 인증 + Swagger 서버 URL 경로 강제 지정
def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema

    openapi_schema = get_openapi(
        title="fast API",
        version="1.0.0",
        description="API with Authorization header",
        routes=app.routes,
    )

    # 🔥 root_path가 Swagger 서버 URL에 반영되도록 수동 삽입
    openapi_schema["servers"] = [{"url": "/api/v1/fastapi"}]

    openapi_schema["components"]["securitySchemes"] = {
        "BearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
        }
    }

    for path in openapi_schema["paths"].values():
        for method in path.values():
            method.setdefault("security", [{"BearerAuth": []}])

    app.openapi_schema = openapi_schema
    return app.openapi_schema

app.openapi = custom_openapi

# ✅ static 파일 mount
app.mount("/static", StaticFiles(directory="app"), name="static")

# ✅ 라우터 등록
app.include_router(upload.router, prefix="/upload", tags=["upload"])
app.include_router(video.router, prefix="/videos", tags=["videos"])
app.include_router(mosaic.router, prefix="/mosaic", tags=["mosaic"])
app.include_router(presigned.router, prefix="/presigned", tags=["presigned"])
app.include_router(video_test.router, prefix="/v1", tags=["videoTest"])