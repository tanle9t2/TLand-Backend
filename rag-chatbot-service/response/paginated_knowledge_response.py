from response.camel_model import CamelModel
from response.knowledge_response import KnowledgeResponse

from typing import List


class PaginatedKnowledgeResponse(CamelModel):
    content: List[KnowledgeResponse]
    total_elements: int
    total_pages: int
    page: int
    size: int
