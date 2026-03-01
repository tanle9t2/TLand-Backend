from datetime import datetime

from response.camel_model import CamelModel


class KnowledgeResponse(CamelModel):
    id: int
    filename: str
    file_url: str
    total_chunks: int
    status: str
    created_at: datetime
