from http import HTTPStatus

from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import py_eureka_client.eureka_client as eureka_client
from llama_cloud_services import LlamaParse
from pydantic import BaseModel
import uvicorn

from request.QueryRequest import QueryRequest
from response.QueryResponse import QueryResponse
from service.embeeding_service import markdown_chunking, markdown_chunking_file
from service.llama_parse_service import parse_markdown
from service.rag_qa import ask_question

app = FastAPI(root_path="/rag-service")


@app.on_event("startup")
async def register_with_eureka():
    await eureka_client.init_async(
        eureka_server="http://localhost:8761/eureka/",
        app_name="rag-chatbot-service",
        instance_port=8000,
        instance_host="127.0.0.1"
    )


@app.post("/api/v1/chat", response_model=QueryResponse)
async def chat_endpoint(request: QueryRequest):
    try:
        # Convert chat_history to list of dicts (if needed by your ask_question)
        chat_history = [{"human": item.human, "ai": item.ai} for item in request.chat_history]
        result = ask_question(request.question, chat_history=chat_history)

        return QueryResponse(
            answer=result["answer"],
            context=result["context"],
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/test")
async def root():
    print("ok")
    return {"message": "Hello World"}


@app.post("/api/v1/feed")
async def feed(file: UploadFile = File(...)):
    await markdown_chunking(file)
    return {
        "message": "Success",
        "code": HTTPStatus.OK
    }

@app.post("/api/v1/test")
async def feed(file: UploadFile = File(...)):
    docs = await markdown_chunking_file(file)
    return {
        "doc": docs,
        "code": HTTPStatus.OK
    }
# @app.post("/api/v1/chunking")
# async def ok(file: UploadFile = File(...)):
#
#     # Chunk Markdown
#     chunks = await markdown_chunking(file, max_chunk_size=500)
#     return {"message": chunks}
#

#
# @app.get("/hello/{name}")
# async def say_hello(name: str):
#     return {"message": f"Hello {name}"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
