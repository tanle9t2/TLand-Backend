from typing import List, Dict, Any
from response.camel_model import CamelModel


class QueryResponse(CamelModel):
    answer: Dict[str, Any]
    context: list
