from http import HTTPStatus
from sqlalchemy.orm import Session

from database import get_db
from entity.knowledge_file import DocType
from request.query_request import QueryRequest
from response.query_response import QueryResponse
from service.embeeding_service import markdown_chunking
from service.knowledge_service import KnowledgeService
from service.rag_qa import ask_question
from fastapi import APIRouter, HTTPException, UploadFile, File, Depends, Form

router = APIRouter(prefix="/api/v1/chat", tags=["Chatbot"])


@router.post("/", response_model=QueryResponse)
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


@router.get("/api/v1/test")
async def root():
    print("ok")
    return {"message": "Hello World"}


@router.post("/feed")
async def feed(file: UploadFile = File(...),
               doc_type: DocType = Form(...),
               db: Session = Depends(get_db)):
    total_chunk = await markdown_chunking(file, doc_type)

    KnowledgeService.create_file(db=db, file=file, filename=file.filename, total_chunks=total_chunk, doc_type=doc_type)
    return {
        "message": "Success",
        "code": HTTPStatus.OK
    }
