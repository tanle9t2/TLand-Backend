from http import HTTPStatus
from sqlalchemy.orm import Session

from database import get_db
from entity.knowledge_file import DocType
from request.query_request import QueryRequest
from response.query_response import QueryResponse
from service.embeeding_service import markdown_chunking, delete_post_from_pinecone, update_source_market_to_legal
from service.knowledge_service import KnowledgeService
from service.rag_qa import ask_question
from fastapi import APIRouter, HTTPException, UploadFile, File, Depends, Form

# from service.post_service import async_post as async_post_grpc

router = APIRouter(prefix="/api/v1/chat", tags=["Chatbot"])


@router.get("/con")
def convert():
    update_source_market_to_legal()
    return {"message": "Deleted successfully"}


@router.post("/", response_model=QueryResponse)
async def chat_endpoint(request: QueryRequest):
    try:
        # Convert chat_history to list of dicts (if needed by your ask_question)
        chat_history = [{"human": item.human, "ai": item.ai} for item in request.chat_history]
        result = await ask_question(request.question, chat_history=chat_history, k=10)

        return QueryResponse(
            answer=result["answer"],
            context=result["context"],
            intent=result["intent"],
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/api/v1/test")
async def root():
    print("ok")
    return {"message": "Hello World"}


@router.delete("/delete-post")
async def delete_post():
    delete_post_from_pinecone()
    return {
        "message": "Success",
        "code": HTTPStatus.OK,
    }


# @router.get("/async-post")
# async def async_post():
#     await async_post_grpc()
#     return {
#         "message": "Success",
#         "code": HTTPStatus.OK,
#     }


@router.post("/feed")
async def feed(file: UploadFile = File(...),
               doc_type: DocType = Form(...),
               db: Session = Depends(get_db)):
    file_content = await file.read()

    new_file = KnowledgeService.create_file(
        db=db,
        file_content=file_content,
        filename=file.filename,
        total_chunks=0,
        doc_type=doc_type)

    total_chunks = await markdown_chunking(file_content, file.filename, doc_type, file_id=new_file.id)
    KnowledgeService.update_file(db=db, file_id=new_file.id, total_chunks=total_chunks)

    return {
        "message": "Success",
        "code": HTTPStatus.OK,
        "data": {
            "id": new_file.id,
            "total_chunks": total_chunks
        }
    }
