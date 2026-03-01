import math

from sqlalchemy import desc, asc
from sqlalchemy.orm import Session
from entity.knowledge_file import KnowledgeFile, FileStatus
from service.s3_service import upload_file_to_s3
from fastapi import File


class KnowledgeService:

    @staticmethod
    def create_file(db: Session, file: File(...), filename: str, total_chunks: int = 0):
        file_url = upload_file_to_s3(file)

        new_file = KnowledgeFile(
            file_url=file_url,
            filename=filename,
            total_chunks=total_chunks,
        )
        print(db)
        db.add(new_file)
        db.commit()
        db.refresh(new_file)

        return new_file

    @staticmethod
    def get_all_files(
            db: Session,
            page: int = 1,
            page_size: int = 10,
            sort_by: str = "id",
            sort_order: str = "desc",
    ):
        query = db.query(KnowledgeFile).filter(KnowledgeFile.status == FileStatus.ACTIVE)

        if hasattr(KnowledgeFile, sort_by):
            column = getattr(KnowledgeFile, sort_by)
            if sort_order.lower() == "desc":
                query = query.order_by(desc(column))
            else:
                query = query.order_by(asc(column))

        total = query.count()

        offset = (page - 1) * page_size
        items = query.offset(offset).limit(page_size).all()

        return {
            "content": items,
            "total_elements": total,
            "page": page,
            "size": page_size,
            "total_pages": math.ceil(total / page_size) if total > 0 else 1
        }

    @staticmethod
    def get_file_by_id(db: Session, file_id: str):
        return db.query(KnowledgeFile).filter(
            KnowledgeFile.id == file_id,
            KnowledgeFile.status == FileStatus.ACTIVE
        ).first()

    @staticmethod
    def update_file(
            db: Session,
            file_id: str,
            filename: str = None,
            total_chunks: int = None,
            status: str = None
    ):
        file = db.query(KnowledgeFile).filter(
            KnowledgeFile.id == file_id
        ).first()

        if not file:
            return None

        if filename is not None:
            file.filename = filename

        if total_chunks is not None:
            file.total_chunks = total_chunks

        if status is not None:
            file.status = status

        db.commit()
        db.refresh(file)

        return file

    @staticmethod
    def delete_file(db: Session, file_id: str):
        file = db.query(KnowledgeFile).filter(
            KnowledgeFile.id == file_id,
            KnowledgeFile.status == FileStatus.ACTIVE
        ).first()

        if not file:
            return False

        file.status = FileStatus.DELETED
        db.commit()

        return True
