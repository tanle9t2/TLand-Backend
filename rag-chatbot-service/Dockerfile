# ── Build Stage: Gen gRPC stubs từ proto files ────────────────────────────────
FROM python:3.11-slim AS proto-gen

WORKDIR /proto-build

RUN pip install --no-cache-dir grpcio-tools==1.74.0 protobuf==6.32.0

COPY proto/ ./proto/


RUN python -m grpc_tools.protoc \
        -I./proto \
        --python_out=./proto \
        --pydantic_out=./proto \
        --grpc_python_out=./proto \
        ./proto/*.proto 2>/dev/null || \
    python -m grpc_tools.protoc \
        -I./proto \
        --python_out=./proto \
        --grpc_python_out=./proto \
        ./proto/*.proto

FROM python:3.11-slim

RUN apt-get update && apt-get install -y \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --gid 1001 appgroup && \
    adduser --uid 1001 --ingroup appgroup --disabled-password --no-create-home appuser

WORKDIR /app


COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY --chown=appuser:appgroup . .

COPY --from=proto-gen --chown=appuser:appgroup /proto-build/proto/ ./proto/

USER 1001

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/docs')" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
