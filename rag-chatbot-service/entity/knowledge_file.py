import enum

from sqlalchemy import Column, String, Integer, DateTime, Enum
from sqlalchemy.sql import func
from database import Base


class FileStatus(str, enum.Enum):
    ACTIVE = "ACTIVE"
    DELETED = "DELETED"


class KnowledgeFile(Base):
    __tablename__ = "knowledge_files"

    id = Column(Integer, primary_key=True, autoincrement=True)
    filename = Column(String, nullable=False)
    file_url = Column(String, nullable=False)
    total_chunks = Column(Integer, nullable=False)
    status = Column(
        Enum(FileStatus),
        default=FileStatus.ACTIVE,
        nullable=False
    )
    created_at = Column(DateTime(timezone=True), server_default=func.now())
