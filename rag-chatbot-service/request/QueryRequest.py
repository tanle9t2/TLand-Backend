from typing import List, Optional
from pydantic import BaseModel

class ChatHistoryItem(BaseModel):
    human: str
    ai: str

class QueryRequest(BaseModel):
    question: str
    chat_history: Optional[List[ChatHistoryItem]] = []
