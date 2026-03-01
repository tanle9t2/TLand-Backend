from http import HTTPStatus

from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import py_eureka_client.eureka_client as eureka_client
from llama_cloud_services import LlamaParse
from pydantic import BaseModel
import uvicorn

from router.knowledge_router import router as knowledge_router
from router.chatbot_router import router as chatbot_router
from router.predict_router import router as predict_router

app = FastAPI(root_path="/rag-service")


@app.on_event("startup")
async def register_with_eureka():
    await eureka_client.init_async(
        eureka_server="http://localhost:8761/eureka/",
        app_name="rag-chatbot-service",
        instance_port=8000,
        instance_host="127.0.0.1"
    )


app.include_router(knowledge_router)
app.include_router(chatbot_router)
app.include_router(predict_router)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
