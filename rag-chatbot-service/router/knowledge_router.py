from fastapi import APIRouter, Depends, HTTPException
from fastapi.params import Query
from sqlalchemy.orm import Session
from database import get_db
from response.knowledge_response import KnowledgeResponse
from response.paginated_knowledge_response import PaginatedKnowledgeResponse
from service.knowledge_service import KnowledgeService

router = APIRouter(prefix="/api/v1/knowledge", tags=["Knowledge"])


@router.get("/", response_model=PaginatedKnowledgeResponse)
def get_all_knowledge(
        page: int = Query(1, ge=1, alias="page"),
        page_size: int = Query(10, ge=1, le=100, alias="pageSize"),
        sort_by: str = Query("id", alias="sortBy"),
        sort_order: str = Query("desc", alias="sortOrder"),
        db: Session = Depends(get_db)
):
    return KnowledgeService.get_all_files(
        db,
        page=page,
        page_size=page_size,
        sort_by=sort_by,
        sort_order=sort_order,
    )


@router.get("/{file_id}", response_model=list[KnowledgeResponse])
def get_knowledge(file_id: str, db: Session = Depends(get_db)):
    file = KnowledgeService.get_file_by_id(db, file_id)
    if not file:
        raise HTTPException(status_code=404, detail="Not found")
    return file


@router.put("/{file_id}")
def update_knowledge(
        file_id: str,
        filename: str = None,
        total_chunks: int = None,
        status: str = None,
        db: Session = Depends(get_db)
):
    file = KnowledgeService.update_file(
        db,
        file_id,
        filename,
        total_chunks,
        status
    )

    if not file:
        raise HTTPException(status_code=404, detail="Not found")

    return file


@router.delete("/{file_id}")
def delete_knowledge(file_id: str, db: Session = Depends(get_db)):
    success = KnowledgeService.delete_file(db, file_id)

    if not success:
        raise HTTPException(status_code=404, detail="Not found")

    return {"message": "Deleted successfully"}
