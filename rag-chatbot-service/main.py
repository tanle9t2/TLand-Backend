from typing import List, Optional
from fastapi import FastAPI, HTTPException, APIRouter
from fastapi.middleware.cors import CORSMiddleware
import py_eureka_client.eureka_client as eureka_client
from pydantic import BaseModel
import uvicorn

from request.QueryRequest import QueryRequest
from response.QueryResponse import QueryResponse
from service.rag_qa import ask_question
from langchain_openai import OpenAIEmbeddings

embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
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
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/test")
async def root():
    print("ok")
    return {"message": "Hello World"}


#
# @app.get("/hello/{name}")
# async def say_hello(name: str):
#     return {"message": f"Hello {name}"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
