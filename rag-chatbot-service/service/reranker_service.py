"""
reranker_service.py
Cohere Rerank integration for post-retrieval document reranking.
Uses rerank-multilingual-v3.0 for Vietnamese language support.
"""

import os
from typing import Optional

import cohere
from dotenv import load_dotenv

load_dotenv()

_RERANK_MODEL = "rerank-multilingual-v3.0"
_client: Optional[cohere.Client] = None


def _get_client() -> cohere.Client:
    """Lazy-init Cohere client (singleton)."""
    global _client
    if _client is None:
        api_key = os.getenv("COHERE_API_KEY")
        if not api_key:
            raise ValueError("COHERE_API_KEY is not set in environment variables.")
        _client = cohere.Client(api_key=api_key)
    return _client


def rerank_documents(query: str, documents: list[str], top_n: int = 5) -> list[str]:
    """
    Rerank a list of document strings by relevance to the query.

    Args:
        query:     User query string.
        documents: List of document strings to rerank.
        top_n:     Number of top documents to return after reranking.

    Returns:
        List of document strings sorted by relevance (most relevant first).
    """
    if not documents:
        return []


    if len(documents) <= top_n:
        return documents

    try:
        client = _get_client()
        response = client.rerank(
            model=_RERANK_MODEL,
            query=query,
            documents=documents,
            top_n=top_n,
        )
        # Return docs in reranked order
        return [documents[result.index] for result in response.results]

    except Exception as e:
        print(f"[reranker_service] Cohere rerank failed: {e}. Returning original order.")
        return documents[:top_n]
