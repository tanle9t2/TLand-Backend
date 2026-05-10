import enum
import os
import re
import uuid

from dotenv import load_dotenv

from langchain_text_splitters import RecursiveCharacterTextSplitter, MarkdownHeaderTextSplitter

import numpy as np
from selenium.webdriver.common.actions.interaction import SOURCE_TYPES
from sklearn.metrics.pairwise import cosine_similarity
from pinecone import Pinecone, ServerlessSpec
from langchain_openai import OpenAIEmbeddings
from langchain_pinecone import PineconeVectorStore

from entity.knowledge_file import DocType
from service.llama_parse_service import parse_markdown

load_dotenv()
embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")

CHUNK_SIZE = 500
CHUNK_OVERLAP = 50
SIMILARITY_THRESHOLD = 0.85
MAX_MERGED_LENGTH = 1500
TABLE_ROW_PATTERN = re.compile(r"^\|.*\|$", re.MULTILINE)
TABLE_SEPARATOR_PATTERN = re.compile(r"^\|[\s\-:|]+\|$", re.MULTILINE)


async def markdown_chunking(file_content, filename, doc_type, file_id=None):
    markdown_text = await parse_markdown(file_content, filename)
    docs = process_markdown(markdown_text, filename, doc_type, file_id)
    await index_to_pinecone(docs)
    return len(docs)


def process_markdown(markdown_text: str, filename: str, doc_type: DocType, file_id=None) -> list[dict]:
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

        header_parts = []
        for key in ("Header 1", "Header 2", "Header 3"):
            val = section.metadata.get(key)
            if val:
                header_parts.append(val)
        header_context = " > ".join(header_parts) if header_parts else ""

        chunks = _smart_markdown_chunking(content)

        for i, chunk in enumerate(chunks):

            contextualized_chunk = chunk
            if header_context:
                contextualized_chunk = f"[{filename} | {header_context}]\n{chunk}"

            chunk_id = f"f_{file_id}_{i}_{uuid.uuid4().hex[:8]}"
            print(
                f"[CHUNK] file={filename} | section={header_context} | "
                f"index={i} | length={len(contextualized_chunk)} | id={chunk_id}"
            )

            docs.append({
                "id": chunk_id,
                "text": contextualized_chunk,
                "metadata": {
                    "id": chunk_id,
                    "knowledge_file_id": file_id,
                    "filename": filename,
                    "section": header_context,
                    "chunk_index": i,
                    "length": len(contextualized_chunk),
                    "source": doc_type,
                    "text": contextualized_chunk,
                }
            })

    return docs


def _smart_markdown_chunking(text: str) -> list[str]:
    blocks = _split_tables_from_text(text)
    all_chunks = []

    for block_type, block_content in blocks:
        if block_type == "table":
            table_chunks = _chunk_table(block_content)
            all_chunks.extend(table_chunks)
        else:
            if block_content.strip():
                text_chunks = _semantic_chunking(block_content)
                all_chunks.extend(text_chunks)

    return all_chunks if all_chunks else [text]


def _split_tables_from_text(text: str) -> list[tuple[str, str]]:
    lines = text.split("\n")
    blocks = []
    current_type = "text"
    current_lines = []

    for line in lines:
        is_table_row = bool(TABLE_ROW_PATTERN.match(line.strip()))

        if is_table_row and current_type != "table":
            # Flush text block
            if current_lines:
                blocks.append(("text", "\n".join(current_lines)))
                current_lines = []
            current_type = "table"
        elif not is_table_row and current_type == "table":
            # Flush table block
            if current_lines:
                blocks.append(("table", "\n".join(current_lines)))
                current_lines = []
            current_type = "text"

        current_lines.append(line)

    # Flush remaining
    if current_lines:
        blocks.append((current_type, "\n".join(current_lines)))

    return blocks


def _chunk_table(table_text: str, max_rows_per_chunk: int = 15) -> list[str]:
    lines = [l for l in table_text.split("\n") if l.strip()]
    if len(lines) <= 2:
        return [table_text]

    # Tìm header (dòng 1) và separator (dòng 2: |---|---|)
    header_line = lines[0]
    separator_idx = None
    for idx, line in enumerate(lines[1:], 1):
        if TABLE_SEPARATOR_PATTERN.match(line.strip()):
            separator_idx = idx
            break

    if separator_idx is None:
        return [table_text]

    header_block = "\n".join(lines[:separator_idx + 1])
    data_rows = lines[separator_idx + 1:]

    if len(data_rows) <= max_rows_per_chunk:
        return [table_text]

    chunks = []
    for start in range(0, len(data_rows), max_rows_per_chunk):
        group = data_rows[start:start + max_rows_per_chunk]
        chunk = header_block + "\n" + "\n".join(group)
        chunks.append(chunk)

    return chunks


def _semantic_chunking(text: str, chunk_size=CHUNK_SIZE, chunk_overlap=CHUNK_OVERLAP,
                       similarity_threshold=SIMILARITY_THRESHOLD,
                       max_merged_length=MAX_MERGED_LENGTH) -> list[str]:
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=["\n\n", "\n", ". ", " ", ""],
    )

    initial_chunks = splitter.split_text(text)
    if not initial_chunks:
        return [text] if text.strip() else []

    embeddings = embedding_model.embed_documents(initial_chunks)

    merged_chunks = []
    i = 0
    while i < len(initial_chunks):
        current_chunk = initial_chunks[i]
        current_emb = embeddings[i]
        j = i + 1
        while j < len(initial_chunks):
            merged_length = len(current_chunk) + len(initial_chunks[j]) + 1
            if merged_length > max_merged_length:
                break
            sim = cosine_similarity([current_emb], [embeddings[j]])[0][0]
            if sim >= similarity_threshold:
                current_chunk += " " + initial_chunks[j]
                current_emb = np.mean([current_emb, embeddings[j]], axis=0)
                j += 1
            else:
                break

        merged_chunks.append(current_chunk)
        i = j

    return merged_chunks


def _format_price(price) -> str:
    try:
        price = float(price)
    except (TypeError, ValueError):
        return "Liên hệ"

    if price <= 0:
        return "Liên hệ"
    if price >= 1_000_000_000:
        ty = price / 1_000_000_000
        return f"{ty:.1f} tỷ VND".replace(".0 ", " ")
    if price >= 1_000_000:
        tr = price / 1_000_000
        return f"{tr:.0f} triệu VND"
    return f"{price:,.0f} VND"


def _safe_value(obj, key, default=None):
    val = obj.get(key) if isinstance(obj, dict) else getattr(obj, key, None)
    return val if val is not None else default


def _build_post_text(post: dict, asset_detail: dict, price, province, ward,
                     land_area, post_type) -> str:
    type_label = "Cho thuê" if post_type == "THUE" else "Bán"

    lines = [
        f"BĐS {type_label}: {post.get('title', '')}",
    ]

    address_parts = [
        asset_detail.get("address", ""),
        ward,
        province,
    ]
    address = ", ".join(p for p in address_parts if p)
    if address:
        lines.append(f"Địa chỉ: {address}")

    formatted_price = _format_price(price)
    lines.append(f"Giá: {formatted_price}")

    if land_area and float(land_area) > 0:
        lines.append(f"Diện tích đất: {land_area} m²")

    usable_area = _safe_value(asset_detail, "usableArea")
    if usable_area:
        lines.append(f"Diện tích sử dụng: {usable_area} m²")

    dimension = _safe_value(asset_detail, "dimension")
    if dimension and isinstance(dimension, (list, tuple)) and len(dimension) >= 2:
        width, length = dimension[0], dimension[1]
        if width and length:
            lines.append(f"Kích thước: {width}m x {length}m")

    properties = _safe_value(asset_detail, "properties")
    if isinstance(properties, dict):
        prop_parts = []
        bedrooms = _safe_value(properties, "bedrooms")
        if bedrooms:
            prop_parts.append(f"{bedrooms} phòng ngủ")
        bathrooms = _safe_value(properties, "bathrooms")
        if bathrooms:
            prop_parts.append(f"{bathrooms} phòng tắm")
        floors = _safe_value(properties, "floors")
        if floors:
            prop_parts.append(f"{floors} tầng")
        if prop_parts:
            lines.append(f"Thông tin: {', '.join(prop_parts)}")

    description = post.get("description", "")
    if description:

        if len(description) > 800:
            description = description[:800] + "..."
        lines.append(f"Mô tả: {description}")

    return "\n".join(lines)


async def feed_db(data):
    docs = []
    for post in data["response"]:
        asset_detail = post["assetDetail"]
        price = post.get("price") or 0
        province = asset_detail.get("province", "")
        address = asset_detail.get("address", "")
        ward = asset_detail.get("ward", "")
        other_info = ", ".join(asset_detail.get("otherInfo", []))
        land_area = asset_detail.get("landArea") or 0
        post_type = "THUE" if str(post.get("type", "")).upper() in ("THUE", "RENT") else "BAN"

        doc_text = _build_post_text(
            post, asset_detail, price, province, ward, land_area, post_type
        )

        properties = _safe_value(asset_detail, "properties") or {}
        legal_status = properties.get("legalDocs")
        furniture_state = properties.get("interiorStatus")
        raw_metadata = {
            "id": post["id"],
            "post_type": post_type,
            "title": post.get("title", ""),
            "asset_detail_id": asset_detail.get("id", "") if asset_detail else "",
            "price": float(price) if price else 0,
            "address": f"{address}, {ward}, {province}",
            "province": province,
            "ward": ward,
            "property_type": properties.get("houseType"),
            "property_feature": other_info,
            "legal_status": legal_status,
            "furniture_state": furniture_state,
            "area": float(land_area) if land_area else 0,
            "bedrooms": int(_safe_value(properties, "bedrooms") or 0),
            "bathrooms": int(_safe_value(properties, "bathrooms") or 0),
            "floors": int(_safe_value(properties, "floors") or 0),
            "source": DocType.POST,
            "text": doc_text,
        }

        docs.append({
            "id": post["id"],
            "text": doc_text,
            "metadata": clean_metadata(raw_metadata),
        })

    await index_to_pinecone(docs)


def clean_metadata(metadata: dict):
    cleaned = {}
    for k, v in metadata.items():
        if v is None:
            continue
        if isinstance(v, (str, int, float, bool)):
            cleaned[k] = v
        elif isinstance(v, list):
            cleaned[k] = [str(x) for x in v]
        else:
            cleaned[k] = str(v)
    return cleaned


async def index_to_pinecone(docs):
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
    index_name = os.getenv("PINECONE_INDEX_NAME")

    if index_name not in pc.list_indexes().names():
        pc.create_index(
            name=index_name,
            dimension=1536,
            metric="cosine",
            spec=ServerlessSpec(cloud="aws", region="us-east-1"),
        )

    index = pc.Index(index_name)
    texts = [doc["text"] for doc in docs]
    vectors = embedding_model.embed_documents(texts)

    upsert_payload = [
        {
            "id": doc["id"],
            "values": vector,
            "metadata": doc["metadata"],
        }
        for doc, vector in zip(docs, vectors)
    ]

    batch_size = 100
    for i in range(0, len(upsert_payload), batch_size):
        index.upsert(vectors=upsert_payload[i: i + batch_size])


def delete_from_pinecone(file_id: int):
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
    index_name = os.getenv("PINECONE_INDEX_NAME")
    index = pc.Index(index_name)

    index.delete(
        filter={
            "knowledge_file_id": {"$eq": int(file_id)}
        }
    )


def update_source_market_to_legal():
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
    index = pc.Index(os.getenv("PINECONE_INDEX_NAME"))

    query_response = index.query(
        vector=[0.0] * 1536,
        top_k=10000,
        include_metadata=True,
        include_values=True,
        filter={"source": {"$eq": "MARKET_ANALYSIS"}}
    )

    matches = query_response.get("matches", [])
    print(f"Found {len(matches)} records")

    batch_size = 100
    batch = []
    total = 0

    for match in matches:
        metadata = match.get("metadata", {})
        values = match.get("values", [])
        vector_id = match.get("id")

        metadata["source"] = "LEGAL"

        batch.append({
            "id": vector_id,
            "values": values,
            "metadata": metadata
        })

        
        if len(batch) >= batch_size:
            index.upsert(vectors=batch)
            print(f"Upserted batch of {len(batch)}")
            total += len(batch)
            batch = []

  
    if batch:
        index.upsert(vectors=batch)
        print(f"Upserted final batch of {len(batch)}")
        total += len(batch)

    print(f"Done! Updated {total} records")


def delete_post_from_pinecone():
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
    index_name = os.getenv("PINECONE_INDEX_NAME")
    index = pc.Index(index_name)

    index.delete(
        filter={
            "source": {"$eq": "POST"}
        }
    )
