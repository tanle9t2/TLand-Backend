import os
import re
from typing import List, Optional
from dotenv import load_dotenv
from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_openai import ChatOpenAI
from langchain_core.documents import Document
from langchain_core.messages import BaseMessage
from langchain_openai import OpenAIEmbeddings
from pinecone import Pinecone
import json

from entity.knowledge_file import DocType
from service.filter_builder import extract_filters
from service.hybrid_search_service import hybrid_search
from service.reranker_service import rerank_documents
from service.price_evaluation_service import price_evaluator

load_dotenv()

llm_model = os.getenv("LLM_MODEL", "gpt-4o-mini")

import os
from pathlib import Path

PROMPT_DIR = Path(__file__).parent.parent / "prompt"


def _load_prompt(filename: str) -> str:
    path = PROMPT_DIR / filename
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


_BASE_PERSONA = _load_prompt("base_persona.txt")
CONSULTATION_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("consultation_prompt.txt")
MARKET_ANALYSIS_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("market_analysis_prompt.txt")
BANK_LOAN_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("bank_loan_prompt.txt")
PROPERTY_DETAIL_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("property_detail_prompt.txt")
LEGAL_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("legal_prompt.txt")
INTENT_DETECTION_PROMPT = _load_prompt("intent_detection_prompt.txt")
QUERY_REFORMULATION_PROMPT = _load_prompt("query_reformulation_prompt.txt")
CLARIFYING_PROMPT = _load_prompt("clarifying_prompt.txt")
OFF_TOPIC_PROMPT = _load_prompt("off_topic_prompt.txt")

_PROPERTY_REF_PATTERN = re.compile(
    r"(c[aă]n\s*(s[oố]|th[uứ])?\s*\d+"  # "căn 1", "căn số 2", "căn thứ 3"
    r"|c[aă]n\s*(đầu\s*tiên|nh[aâ]́t|đ[oó]|n[aà]y|tr[eê]́n)"  # "căn đầu tiên", "căn đó", "căn này"
    r"|đ[aá]nh\s*gi[aá]\s*c[aă]n"  # "đánh giá căn"
    r"|chi\s*ti[eế]t\s*c[aă]n"  # "chi tiết căn"
    r"|xem\s*(th[eê]m|c[aă]n|n[hờ][aà])\s*(đ[oó]|n[aà]y|\d+)"  # "xem thêm căn đó"
    r")",
    re.IGNORECASE | re.UNICODE,
)

_PRICE_PATTERN = re.compile(
    r"(\d+[.,]?\d*\s*(tỷ|triệu|billion|tr))"
    r"|(ngân sách|budget|giá|khoảng giá|tầm giá|dưới|trên|từ.*đến)",
    re.IGNORECASE | re.UNICODE,
)

_LOCATION_PATTERN = re.compile(
    r"(quận|huyện|phường|đường|tp\.|thành phố|tỉnh"
    r"|bình thạnh|gò vấp|tân bình|thủ đức|bình dương|đồng nai"
    r"|quận\s*\d+|q\.?\s*\d+)",
    re.IGNORECASE | re.UNICODE,
)


def _has_price(text: str) -> bool:
    return bool(_PRICE_PATTERN.search(text))


def _has_location(text: str) -> bool:
    return bool(_LOCATION_PATTERN.search(text))


async def detect_intent(query: str, standalone_query: str, llm: ChatOpenAI, chat_history=None) -> str:
    if _PROPERTY_REF_PATTERN.search(query):
        has_properties_in_history = False
        if chat_history:
            for msg in reversed(chat_history):
                if isinstance(msg, dict):
                    ai_data = msg.get("ai", {})
                    if isinstance(ai_data, dict) and ai_data.get("properties"):
                        has_properties_in_history = True
                        break
        if has_properties_in_history:
            print(f"[DEBUG] Rule-based pre-check: PROPERTY_DETAIL (ordinal ref in query + history has properties)")
            return "PROPERTY_DETAIL"

    prompt = ChatPromptTemplate.from_messages([
        ("system", INTENT_DETECTION_PROMPT),
        ("human", "Câu hỏi gốc: {query}\nCâu diễn giải có ngữ cảnh: {standalone_query}")
    ])
    chain = prompt | llm
    response = await chain.ainvoke({"query": query, "standalone_query": standalone_query})
    intent = response.content.strip().upper()

    if "MARKET_ANALYSIS" in intent:
        return "MARKET_ANALYSIS"
    if "BANK_LOAN" in intent:
        return "BANK_LOAN"
    if "PROPERTY_DETAIL" in intent:
        return "PROPERTY_DETAIL"
    if "LEGAL" in intent:
        return "LEGAL"
    if "OFF_TOPIC" in intent:
        return "OFF_TOPIC"
    if "UNCLEAR" in intent:
        return "UNCLEAR"
    return "CONSULTATION"


async def _ask_clarifying_question(query: str, standalone_query: str, llm: ChatOpenAI) -> dict:
    prompt = ChatPromptTemplate.from_messages([
        ("system", CLARIFYING_PROMPT),
        ("human", "Yêu cầu gốc: {query}\nDiễn giải: {standalone_query}")
    ])
    chain = prompt | llm.bind(response_format={"type": "json_object"})
    try:
        response = await chain.ainvoke({"query": query, "standalone_query": standalone_query})
        return json.loads(response.content)
    except Exception as exc:
        print(f"[DEBUG] {exc}")

        return {
            "message": "Dạ để em tìm được BĐS phù hợp nhất, anh/chị vui lòng cho em biết thêm khu vực và ngân sách khoảng bao nhiêu ạ?"
        }


def _resolve_property_from_history(chat_history: list, query: str) -> Optional[str]:
    if not chat_history:
        return None

    properties = []
    for msg in reversed(chat_history):
        if isinstance(msg, dict):
            ai_data = msg.get("ai", {})
            if isinstance(ai_data, dict):
                props = ai_data.get("properties", [])
                if props:
                    properties = props
                    break

    if not properties:
        return None

    q = query.lower()

    match = re.search(r"c[aă]n\s*(?:s[oố]|th[uứ])?\s*(\d+)", q)
    if match:
        idx = int(match.group(1)) - 1
        if 0 <= idx < len(properties):
            return properties[idx].get("propertyId")

    # Word ordinals
    ordinal_map = {"đầu tiên": 0, "nhất": 0, "hai": 1, "ba": 2, "bốn": 3, "năm": 4}
    for word, idx in ordinal_map.items():
        if word in q and 0 <= idx < len(properties):
            return properties[idx].get("propertyId")

    # Fallback: reference to single item in list
    if len(properties) == 1:
        return properties[0].get("propertyId")

    return None


def _fetch_pinecone_entry_by_id(property_id: str) -> Optional[dict]:
    """
    Fetch full vector entry from Pinecone by ID.
    Returns {"text": str, "metadata": dict} or None if not found.
    """
    try:
        pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
        index = pc.Index(os.getenv("PINECONE_INDEX_NAME"))
        response = index.fetch(ids=[property_id])
        vectors = response.vectors
        if property_id in vectors:
            metadata = vectors[property_id].metadata or {}
            text = metadata.get("text") or metadata.get("content", "")
            return {"text": text, "metadata": metadata}
        return None
    except Exception as e:
        print(f"[DEBUG] _fetch_pinecone_entry_by_id error: {e}")
        return None


async def ask_question(query: str, chat_history=None, k: int = 5):
    llm = ChatOpenAI(model=llm_model, temperature=0)
    formatted_history = _format_chat_history(chat_history)

    if len(formatted_history) > 10:
        formatted_history = formatted_history[-10:]

    contextualize_q_prompt = ChatPromptTemplate.from_messages([
        ("system", QUERY_REFORMULATION_PROMPT),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])

    if formatted_history:
        contextualize_q_chain = contextualize_q_prompt | llm
        standalone_query = (await contextualize_q_chain.ainvoke({
            "input": query,
            "chat_history": formatted_history
        })).content
    else:
        standalone_query = query
    print(f"[DEBUG] query: {query}")
    print(f"[DEBUG] standalone_query: {standalone_query}")

    intent = await detect_intent(query, standalone_query, llm, chat_history)
    print(f"[DEBUG] Detected intent: {intent}")

    if intent == "OFF_TOPIC":
        print("[DEBUG] Intent=OFF_TOPIC — generating dynamic response")
        prompt = ChatPromptTemplate.from_messages([
            ("system", OFF_TOPIC_PROMPT),
            ("human", "{query}")
        ])
        chain = prompt | llm.bind(response_format={"type": "json_object"})
        try:
            response = await chain.ainvoke({"query": query})
            off_topic_answer = json.loads(response.content)
        except Exception:
            off_topic_answer = {
                "message": "Xin lỗi anh/chị, chuyên môn của em là tư vấn **Bất động sản**. Anh/chị có đang quan tâm đến khu vực hay dự án nào không ạ?"
            }
        return {
            "intent": intent,
            "answer": {**off_topic_answer, "properties": []},
            "context": [],
        }

    if intent == "UNCLEAR":
        clarification = await _ask_clarifying_question(query, standalone_query, llm)
        return {
            "intent": intent,
            "answer": {**clarification, "properties": []},
            "context": [],
        }

    if intent == "CONSULTATION":
        combined = f"{query} {standalone_query}"
        has_loc = _has_location(combined)
        has_price = _has_price(combined)
        print(f"[DEBUG] CONSULTATION gate — has_location={has_loc}, has_price={has_price}")

        if not has_loc or not has_price:
            print("[DEBUG] CONSULTATION gate — missing fields, generating dynamic clarifying question")
            clarification = await _ask_clarifying_question(query, standalone_query, llm)
            return {
                "intent": "UNCLEAR",
                "answer": {**clarification, "properties": []},
                "context": [],
            }

    intent_config = {
        "MARKET_ANALYSIS": (MARKET_ANALYSIS_PROMPT, DocType.MARKET_ANALYSIS),
        "BANK_LOAN": (BANK_LOAN_PROMPT, DocType.BANK_LOAN),
        "PROPERTY_DETAIL": (PROPERTY_DETAIL_PROMPT, DocType.POST),
        "LEGAL": (LEGAL_PROMPT, DocType.LEGAL),
    }

    selected_prompt, source = intent_config.get(
        intent, (CONSULTATION_PROMPT, DocType.POST)
    )
    pinecone_filter = {"source": {"$eq": source}}

    if intent != "BANK_LOAN" and intent != "LEGAL":
        pinecone_filter = extract_filters(pinecone_filter, standalone_query)

    print(f"[DEBUG] Pinecone filter: {pinecone_filter}")

    resolved_property_entry: Optional[dict] = None
    if intent == "PROPERTY_DETAIL":
        resolved_id = _resolve_property_from_history(chat_history or [], query)
        print(f"[DEBUG] PROPERTY_DETAIL: chat_history = {chat_history}")
        if resolved_id:
            print(f"[DEBUG] PROPERTY_DETAIL: resolved_id = {resolved_id}")
            resolved_property_entry = _fetch_pinecone_entry_by_id(resolved_id)
            if resolved_property_entry:
                print(f"[DEBUG] PROPERTY_DETAIL: fetched from Pinecone by ID, skipping hybrid search")

    if resolved_property_entry is None:
        raw_results = hybrid_search(
            query=standalone_query,
            pinecone_filter=pinecone_filter,
            vector_top_k=20,
            final_top_k=10,
        )
        print(f"[DEBUG] Raw results count: {len(raw_results)}")
        reranked_docs = rerank_documents(query=standalone_query, documents=raw_results, top_n=k)
        print(f"[DEBUG] Reranked docs count: {len(reranked_docs)}")
    else:
        reranked_docs = []
        print(f"[DEBUG] Hybrid search SKIPPED — using direct Pinecone fetch")

    docs = []
    enriched_texts = []

    # If PROPERTY_DETAIL was resolved by ID, inject the fetched entry directly
    if resolved_property_entry is not None:
        entry_metadata = resolved_property_entry["metadata"]
        entry_text = resolved_property_entry["text"]
        post_id = entry_metadata.get("id", resolved_id)
        page_content = f"[Mã BĐS: {post_id}]\n{entry_text}"
        docs.append(Document(page_content=page_content))
        enriched_texts.append({
            "id": post_id,
            "text": entry_text,
            "metadata": entry_metadata,
            "rrf_score": 1.0,  # synthetic score — direct fetch
        })
        print(f"[DEBUG] Injected direct-fetch doc for propertyId={post_id}")
    else:
        BANK_LOAN_RRF_THRESHOLD = 0.01
        for i, item in enumerate(reranked_docs, 1):
            text = item.get("text", "")
            metadata = item.get("metadata", {})
            rrf_score = item.get("rrf_score", 0.0)

            if intent == "BANK_LOAN" and rrf_score < BANK_LOAN_RRF_THRESHOLD:
                print(f"[DEBUG] BANK_LOAN doc {i}: score={rrf_score:.4f} — SKIPPED (below threshold)")
                continue

            post_id = metadata.get("id", "UNKNOWN")
            page_content = f"[Mã BĐS: {post_id}]\n{text}"
            print(f"\n===== Document {i} =====")
            print(f"[DEBUG] score={rrf_score:.4f}")
            print(page_content.strip()[:200])

            docs.append(Document(page_content=page_content))
            enriched_texts.append({
                "id": post_id,
                "text": text,
                "metadata": metadata,
                "rrf_score": rrf_score
            })

    print(f"[DEBUG] Docs passed to LLM: {len(docs)}")

    if not docs:
        if intent == "MARKET_ANALYSIS":
            docs.append(Document(
                page_content=(
                    "Không có dữ liệu tham khảo từ thị trường. "
                    "Hãy dựa hoàn toàn vào [KẾT QUẢ ĐÁNH GIÁ TỪ MÔ HÌNH DỰ BÁO] để phân tích. "
                    "Nếu mô hình thiếu thông tin, hãy hỏi người dùng bổ sung các trường còn thiếu."
                )
            ))
        elif intent == "BANK_LOAN":
            docs.append(Document(
                page_content=(
                    "KHÔNG CÓ DỮ LIỆU GÓI VAY — Hệ thống hiện chưa có tài liệu về các sản phẩm vay ngân hàng.\n"
                    "TUYỆT ĐỐI KHÔNG tự sáng tác hoặc bịa ra bất kỳ thông tin gói vay nào.\n"
                    "Hãy thông báo lịch sự cho khách rằng hệ thống chưa cập nhật dữ liệu gói vay,\n"
                    "và gợi ý khách liên hệ trực tiếp với ngân hàng hoặc chuyên viên tài chính để được tư vấn chính xác."
                )
            ))
        elif intent == "LEGAL":
            docs.append(Document(
                page_content=(
                    "KHÔNG CÓ DỮ LIỆU PHÁP LÝ — Hệ thống hiện chưa có tài liệu pháp luật liên quan đến câu hỏi này.\n"
                    "TUYỆT ĐỐI KHÔNG tự bịa ra điều luật, điều khoản, hoặc văn bản pháp quy nào.\n"
                    "Hãy thông báo lịch sự cho khách rằng hệ thống chưa có dữ liệu pháp lý về vấn đề này"
                )
            ))
        else:
            docs.append(Document(
                page_content=(
                    "KHÔNG CÓ DỮ LIỆU — Hệ thống không tìm thấy BĐS nào phù hợp với tiêu chí tìm kiếm.\n"
                    "KHÔNG ĐƯỢC bịa ra bất kỳ BĐS nào.\n"
                    "Hãy thông báo cho người dùng và gợi ý CỤ THỂ:\n"
                    "1. Khu vực lân cận có thể có BĐS phù hợp hơn\n"
                    "2. Điều chỉnh khoảng giá (tăng/giảm)\n"
                    "3. Thử loại hình BĐS khác (căn hộ thay nhà phố, v.v.)\n"
                    "4. Bỏ bớt tiêu chí lọc để mở rộng kết quả"
                )
            ))
        print("[DEBUG] No docs found — injected empty-context marker")

    if intent == "MARKET_ANALYSIS":
        # If user references a property from chat history, fetch its full metadata
        eval_text = standalone_query
        resolved_id = _resolve_property_from_history(chat_history or [], query)
        if resolved_id:
            print(f"[DEBUG] MARKET_ANALYSIS: resolved propertyId={resolved_id}")
            pinecone_entry = _fetch_pinecone_entry_by_id(resolved_id)
            if pinecone_entry:
                eval_text = pinecone_entry["metadata"]
                print(f"[DEBUG] Using Pinecone stored text for price evaluation")

        query_eval = await price_evaluator.evaluate(eval_text)

        if query_eval.get("status") != "unknown":
            missing = query_eval.get("missing_fields", [])
            missing_str = ", ".join(missing) if missing else "Không"

            adhoc_doc_content = (
                f"[THÔNG TIN BĐS NGƯỜI DÙNG CUNG CẤP TRONG CÂU HỎI]:\n{standalone_query}\n\n"
                f"[KẾT QUẢ ĐÁNH GIÁ TỪ MÔ HÌNH DỰ BÁO]:\n"
                f"- Trạng thái: {query_eval.get('status')}\n"
                f"- Giá dự báo thị trường: {query_eval.get('predicted_price_ty', 0):.2f} Tỷ VND\n"
                f"- Chênh lệch: {query_eval.get('diff_percent', 0):.1f}% so với giá thực tế người dùng đưa ra\n"
                f"- Các trường thông tin còn thiếu: {missing_str}"
            )
            docs.insert(0, Document(page_content=adhoc_doc_content))
            print("Predict", adhoc_doc_content)

    elif intent == "PROPERTY_DETAIL" and docs:
        # Prefer structured Pinecone metadata for accuracy — skip LLM extraction
        top_metadata = enriched_texts[0]["metadata"] if enriched_texts else {}
        print(f"[DEBUG] PROPERTY_DETAIL metadata: {top_metadata}")

        if top_metadata:
            prop_eval = await price_evaluator.evaluate_from_metadata(top_metadata)
        else:
            # Fallback: parse from text (less accurate)
            prop_eval = await price_evaluator.evaluate(docs[0].page_content)

        missing = prop_eval.get("missing_fields", [])
        missing_str = ", ".join(missing) if missing else "Không"
        predicted = prop_eval.get('predicted_price_ty', 0)
        diff = prop_eval.get('diff_percent', 0)
        status = prop_eval.get('status', 'Chưa đủ thông tin')

        eval_doc_content = (
            f"[KẾT QUẢ ĐÁNH GIÁ TỪ MÔ HÌNH DỰ BÁO]:\n"
            f"- Trạng thái: {status}\n"
            f"- Giá dự báo thị trường: {predicted:.2f} Tỷ VND\n"
            f"- Chênh lệch so với giá niêm yết: {diff:.1f}%\n"
            f"- Các trường thông tin còn thiếu: {missing_str}"
        )
        docs.insert(0, Document(page_content=eval_doc_content))
        print(f"[DEBUG] Property detail eval: {eval_doc_content}")

    raw_context = "\n\n".join([doc.page_content for doc in docs])
    has_real_data = bool(enriched_texts)

    if intent == "CONSULTATION" and has_real_data:
        context_str = f"CÓ DỮ LIỆU — Hệ thống tìm được {len(enriched_texts)} BĐS. BẮT BUỘC giới thiệu ít nhất 1 căn.\n\n{raw_context}"
    else:
        context_str = raw_context

    qa_prompt = ChatPromptTemplate.from_messages([
        ("system", selected_prompt),
        MessagesPlaceholder("chat_history"),
        ("human", "Lịch sử ngữ cảnh:\n{context}\n\nCâu hỏi: {input}")
    ])

    if intent in ("BANK_LOAN", "MARKET_ANALYSIS", "LEGAL"):
        chain = qa_prompt | llm.bind(response_format={"type": "json_object"})
    else:
        chain = qa_prompt | llm

    try:
        response = await chain.ainvoke({
            "input": query,
            "chat_history": formatted_history,
            "context": context_str,
        })
        print(f"[DEBUG] raw ans: {response.content}")

        try:

            content = response.content.strip()

            start_idx = content.find('{')
            end_idx = content.rfind('}')

            if start_idx != -1 and end_idx != -1 and start_idx < end_idx:
                content = content[start_idx:end_idx + 1]

            parsed_answer = json.loads(content)
        except json.JSONDecodeError as je:
            print(f"[rag_qa] JSON Parse Error: {je}")

            parsed_answer = {
                "message": response.content,
                "properties": []
            }

        return {
            "intent": intent,
            "answer": parsed_answer,
            "context": enriched_texts,
        }

    except Exception as e:
        print(f"[rag_qa] Error: {e}")
        return {
            "intent": intent,
            "answer": {
                "message": "Xin lỗi, tôi gặp sự cố kỹ thuật. Vui lòng thử lại sau.",
                "properties": []
            },
            "context": [],
        }


def get_query_vector(query: str):
    embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
    return embedding_model.embed_query(query)


def _format_chat_history(chat_history) -> List[BaseMessage]:
    if not chat_history:
        return []
    formatted = []
    for msg in chat_history:
        if isinstance(msg, dict):
            if msg.get("human"):
                formatted.append(HumanMessage(content=msg["human"]))
            if msg.get("ai"):
                ai_data = msg["ai"]
                if isinstance(ai_data, dict):
                    ai_text = ai_data.get("message", "")
                    # Append ordinal property summary so LLM can resolve "căn thứ 2"
                    properties = ai_data.get("properties", [])
                    if properties:
                        summary_lines = []
                        for idx, prop in enumerate(properties, 1):
                            pid = prop.get("propertyId", "")
                            title = prop.get("title", "")
                            price = prop.get("price", "")
                            summary_lines.append(f"  Căn {idx}: [Mã BĐS: {pid}] {title} - {price}")
                        ai_text += "\n[Danh sách BĐS đã giới thiệu trong lượt trước]\n" + "\n".join(summary_lines)
                else:
                    ai_text = str(ai_data)
                formatted.append(AIMessage(content=ai_text))
        elif hasattr(msg, "content"):
            formatted.append(msg)
    return formatted
