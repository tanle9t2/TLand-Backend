from typing import List

from pydantic import BaseModel


class QueryResponse(BaseModel):
    answer: str
    context: list

