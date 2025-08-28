import os
import uuid

from dotenv import load_dotenv
from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter, MarkdownHeaderTextSplitter

import numpy as np
from langchain_experimental.text_splitter import SemanticChunker
from sklearn.metrics.pairwise import cosine_similarity
from pinecone import Pinecone, ServerlessSpec
from langchain_openai import OpenAIEmbeddings
from langchain_pinecone import PineconeVectorStore

from service.llama_parse_service import parse_markdown

load_dotenv()
embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")


async def markdown_chunking(file):
    markdown_text = await parse_markdown(file)

    headers_to_split_on = [
        ("#", "Header 1"),
        ("##", "Header 2"),
        ("###", "Header 3"),
    ]
    md_splitter = MarkdownHeaderTextSplitter(
        headers_to_split_on=headers_to_split_on,
        strip_headers=False
    )
    md_sections = md_splitter.split_text(markdown_text)
    docs = []

    for section in md_sections:
        content = section.page_content.strip()
        if not content:
            continue
        chunks = semantic_chunking(content)

        for i, chunk in enumerate(chunks):
            docs.append({
                "id": f"{file.filename}_{i}_{uuid.uuid4().hex}",  # unique ID
                "text": chunk,
                "metadata": {
                    "filename": file.filename,
                    "chunk_index": i,
                    "length": len(chunk),
                }
            })

    await index_to_pinecone(docs)


def semantic_chunking(text, chunk_size=500, chunk_overlap=50, similarity_threshold=0.85):
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size
        , chunk_overlap=chunk_overlap
        , separators=["\n\n", "\n", ".", " ", ""], )

    initial_chunks = splitter.split_text(text)
    embeddings = embedding_model.embed_documents(initial_chunks)

    merged_chunks = []
    i = 0
    while i < len(initial_chunks):
        current_chunk = initial_chunks[i]
        current_emb = embeddings[i]
        j = i + 1
        while j < len(initial_chunks):
            sim = cosine_similarity([current_emb], [embeddings[j]])[0][0]
            if sim >= similarity_threshold:
                # Merge similar chunks
                current_chunk += " " + initial_chunks[j]
                # Average embeddings
                current_emb = np.mean([current_emb, embeddings[j]], axis=0)
                j += 1
            else:
                break

        merged_chunks.append(current_chunk)
        i = j

    return merged_chunks


async def feed_db(data):
    docs = []
    for post in data["response"]:
        asset_detail = post["assetDetail"]
        doc_text = f"""
        Title: {post.get('title')}
        Description: {post.get('description')}
        Address: {asset_detail.get('address', '')}, {asset_detail.get('ward', '')}, {asset_detail.get('province', '')}
        Price: {post.get('price')}
        Properties: {asset_detail.get('properties')}
        Type: {post.get('type')}
        Status: {post.get('status')},
        LandArea: {asset_detail.get('landArea')},
        Width:{asset_detail.get('dimension')[0]},
        Length:{asset_detail.get('dimension')[1]},
        UsableArea: {asset_detail.get('usableArea')},
        """
        docs.append({
            "id": post["id"],  # keep original ID
            "text": doc_text,  # chunked text
            "metadata": {
                'id': post["id"],
                'type': post['type'],
                'title': post['title'],
                'assetDetailId': post['assetDetail']['id'],
            }
        })

    await index_to_pinecone(docs)


async def index_to_pinecone(docs):
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))

    index_name = os.getenv("PINECONE_INDEX_NAME")
    if index_name not in pc.list_indexes().names():
        pc.create_index(
            name=index_name,
            dimension=1536,  # for OpenAI embeddings
            metric="cosine",
            spec=ServerlessSpec(cloud="aws", region="us-east-1")
        )
    # Embedding model

    PineconeVectorStore.from_texts(
        texts=[doc["text"] for doc in docs],
        embedding=embedding_model,
        metadatas=[doc["metadata"] for doc in docs],
        index_name=index_name,
    )
