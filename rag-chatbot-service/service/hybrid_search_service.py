import re
import os
from typing import Optional

from dotenv import load_dotenv
from langchain_openai import OpenAIEmbeddings
from pinecone import Pinecone
from rank_bm25 import BM25Okapi

load_dotenv()

_EMBEDDING_MODEL = "text-embedding-3-small"
_RRF_K = 60  # RRF smoothing constant (standard value)


def _tokenize(text: str) -> list[str]:
    return re.findall(r"\w+", text.lower())


def hybrid_search(
        query: str,
        pinecone_filter: Optional[dict] = None,
        vector_top_k: int = 20,
        final_top_k: int = 10,
) -> list[dict]:
    """
    Perform hybrid search: vector (Pinecone) + BM25, fused with RRF.

    Args:
        query:          User query string.
        pinecone_filter: Pinecone metadata filter dict (from filter_builder).
        vector_top_k:   Number of docs to fetch from Pinecone for candidate pool.
        final_top_k:    Number of docs to return after RRF fusion.

    Returns:
        List of dicts: [{"text": str, "metadata": dict, "rrf_score": float}, ...]
    """
    # 1. Vector search — fetch candidate pool
    candidates = _vector_search(query, pinecone_filter, k=vector_top_k)
    print(f"[DEBUG] Pinecone search: {len(candidates)}")
    if not candidates:
        return []

    corpus = [doc["text"] for doc in candidates]
    bm25_scores = _bm25_score(query, corpus)

    vector_ranks = {i: i + 1 for i in range(len(candidates))}

    # BM25 ranks: sort by BM25 score descending
    bm25_order = sorted(range(len(candidates)), key=lambda i: bm25_scores[i], reverse=True)
    bm25_ranks = {doc_idx: rank + 1 for rank, doc_idx in enumerate(bm25_order)}

    # 4. RRF fusion
    rrf_scores = {}
    for i in range(len(candidates)):
        vector_rank = vector_ranks.get(i, len(candidates) + 1)
        bm25_rank = bm25_ranks.get(i, len(candidates) + 1)
        rrf_scores[i] = 1.0 / (_RRF_K + vector_rank) + 1.0 / (_RRF_K + bm25_rank)

    sorted_indices = sorted(rrf_scores.keys(), key=lambda i: rrf_scores[i], reverse=True)
    top_indices = sorted_indices[:final_top_k]

    return [
        {
            "text": candidates[i]["text"],
            "metadata": candidates[i]["metadata"],
            "rrf_score": rrf_scores[i],
        }
        for i in top_indices
    ]


def _vector_search(query: str, pinecone_filter: Optional[dict], k: int) -> list[dict]:
    """Query Pinecone and return raw results."""
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
    index = pc.Index(os.getenv("PINECONE_INDEX_NAME"))

    embedding_model = OpenAIEmbeddings(model=_EMBEDDING_MODEL)
    query_vector = embedding_model.embed_query(query)

    query_kwargs = {
        "vector": query_vector,
        "top_k": k,
        "include_metadata": True,
    }
    if pinecone_filter:
        query_kwargs["filter"] = pinecone_filter

    response = index.query(**query_kwargs)

    results = []
    for match in response.get("matches", []):
        metadata = match.get("metadata", {})
        text = metadata.get("text") or metadata.get("content", "")
        results.append({"text": text, "metadata": metadata, "vector_score": match.get("score", 0.0)})

    return results


def _bm25_score(query: str, corpus: list[str]) -> list[float]:
    """Compute BM25 scores for each document in corpus given the query."""
    tokenized_corpus = [_tokenize(doc) for doc in corpus]
    bm25 = BM25Okapi(tokenized_corpus)
    return list(bm25.get_scores(_tokenize(query)))
