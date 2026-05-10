from typing import List, Dict, Any
from response.camel_model import CamelModel


class QueryResponse(CamelModel):
    intent: str
    answer: Dict[str, Any]
    context: list
