from typing import List, Optional, Any
from pydantic import BaseModel

class ChatHistoryItem(BaseModel):
    human: str
    ai: Any

class QueryRequest(BaseModel):
    question: str
    chat_history: Optional[List[ChatHistoryItem]] = []
