import os
from dotenv import load_dotenv
from langchain.schema import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.chains import create_history_aware_retriever, create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_openai import ChatOpenAI
from langchain.schema import Document
import json

from entity.knowledge_file import DocType
from service.filter_builder import extract_filters
from service.hybrid_search_service import hybrid_search
from service.reranker_service import rerank_documents
from service.price_evaluation_service import price_evaluator
from typing import List
from langchain_core.messages import BaseMessage
from langchain_openai import OpenAIEmbeddings

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
_CONSULTATION_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("consultation_prompt.txt")
_MARKET_ANALYSIS_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("market_analysis_prompt.txt")
_BANK_LOAN_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("bank_loan_prompt.txt")
_PROPERTY_DETAIL_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("property_detail_prompt.txt")


def _detect_intent(query: str, standalone_query: str, llm: ChatOpenAI) -> str:
    prompt = ChatPromptTemplate.from_messages([
        ("system", "Phân loại yêu cầu của người dùng vào 1 trong 4 loại:\n"
                   "'MARKET_ANALYSIS': hỏi đắt rẻ, so sánh giá, nhận định thị trường BĐS.\n"
                   "'BANK_LOAN': hỏi về vay vốn ngân hàng, thiếu tiền, lãi suất.\n"
                   "'PROPERTY_DETAIL': muốn xem chi tiết về 1 căn cụ thể đã được đề cập (VD: 'chi tiết căn 1', 'cho tui xem thêm căn đó', 'căn thứ 2 thế nào').\n"
                   "'CONSULTATION': tìm nhà, hỏi thông tin chung, tìm kiếm BĐS.\n"
                   "Chỉ trả về đúng 1 từ khóa duy nhất."),
        ("human", "Câu hỏi gốc: {query}\nCâu diễn giải có ngữ cảnh: {standalone_query}")
    ])
    chain = prompt | llm
    response = chain.invoke({"query": query, "standalone_query": standalone_query})
    intent = response.content.strip().upper()

    if "MARKET_ANALYSIS" in intent:
        return "MARKET_ANALYSIS"
    if "BANK_LOAN" in intent:
        return "BANK_LOAN"
    if "PROPERTY_DETAIL" in intent:
        return "PROPERTY_DETAIL"
    return "CONSULTATION"


def ask_question(query: str, chat_history=None, k: int = 5):
    """
      1. Auto-extract metadata filters from query
      2. Hybrid search (vector + BM25 + RRF) with filters
      3. Cohere reranker
      4. LLM answer generation
    """
    llm = ChatOpenAI(model=llm_model, temperature=0)
    formatted_history = _format_chat_history(chat_history)

    if len(formatted_history) > 10:
        formatted_history = formatted_history[-10:]

    contextualize_q_prompt = ChatPromptTemplate.from_messages([
        (
            "system",
            "Bạn là chuyên gia tái cấu trúc truy vấn (query reformulation) cho hệ thống AI tìm kiếm Bất động sản (BĐS).\n\n"
            "--- NHIỆM VỤ ---\n"
            "Đọc toàn bộ lịch sử hội thoại và câu mới nhất của người dùng. Viết lại nội dung mới nhất thành MỘT CÂU ĐỘC LẬP (standalone query) chứa đầy đủ ngữ cảnh từ lịch sử NHƯNG BẮT BUỘC PHẢI PHẢN ÁNH ĐÚNG Ý ĐỊNH TÌM KIẾM HIỆN TẠI.\n\n"
            "--- QUY TẮC (CONSTRAINTS) ---\n"
            "1. HÌNH THỨC OUTPUT: CHỈ trả về đúng một chuỗi văn bản duy nhất của câu được viết lại. TUYỆT ĐỐI KHÔNG giải thích, KHÔNG có từ nối (VD: 'Câu hỏi là:', 'Đây là...').\n"
            "2. KẾ THỪA (CARRY-OVER): Mang theo các định danh (Khu vực, Loại hình, Tên đường...) từ câu trước nếu ngữ cảnh vẫn đang tiếp diễn.\n"
            "3. GHI ĐÈ (OVERRIDE): Nếu câu mới nhất có đề cập thay đổi mâu thuẫn với câu cũ (VD: đổi ngân sách từ 5 tỷ thành 7 tỷ), PHẢI ƯU TIÊN dùng tiêu chí MỚI NHẤT.\n"
            "4. MỞ RỘNG (BROADEN): Nếu câu mới ngụ ý tìm kiếm mành lưới mở rộng (VD: 'Vậy trong khu vực này đang có các bđs nào', 'tất cả bđs ở Gò Vấp', 'gợi ý cho tôi các căn khác'), BẮT BUỘC PHẢI LOẠI BỎ (DROP) các điều kiện giới hạn cũ (như ngân sách cũ, gò bó diện tích cũ) để có thể truy vấn bức tranh toàn cảnh.\n"
            "5. BẢO TOÀN Ý ĐỊNH (INTENT PRESERVATION): Giữ nguyên bản chất của câu hỏi. Nếu là câu hỏi giá đắt rẻ, phàn nàn thiếu tiền vay vốn, hoặc chốt mua, cần phải giữ đúng sắc thái đó.\n\n"
            "--- VÍ DỤ MINH HỌA (FEW-SHOT EXAMPLES) ---\n"
            "Lịch sử: [User: 'Tôi muốn tìm căn hộ tại Quận 2'] \n"
            "Câu mới: 'Có những căn nào tầm 3 tỷ không?'\n"
            "-> Output: Tìm căn hộ tại Quận 2 có mức giá khoảng 3 tỷ.\n\n"

            "Lịch sử: [User: 'Tìm căn hộ Quận 2 tầm 3 tỷ', AI: 'Hiện chưa có căn 3 tỷ...'] \n"
            "Câu mới: 'Thế khu đó hiện đang có các căn nào?'\n"
            "-> Output: Tìm kiếm tất cả các căn hộ hiện đang có trong khu vực Quận 2.\n\n"

            "Lịch sử: [User: 'Nhà hẻm ở quận 9 giá 5 tỷ'] \n"
            "Câu mới: 'Giá như vậy mua có bị hớ không?'\n"
            "-> Output: Đánh giá xem mức giá 5 tỷ cho ngôi nhà hẻm ở Quận 9 là đắt hay rẻ so với thị trường.\n\n"

            "--- LƯU Ý CUỐI ---\n"
            "Đảm bảo OUTPUT cuối cùng chuẩn xác, ngắn gọn và là dạng câu hỏi đơn độc lập."
        ),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])

    if formatted_history:
        contextualize_q_chain = contextualize_q_prompt | llm
        standalone_query = contextualize_q_chain.invoke({
            "input": query,
            "chat_history": formatted_history
        }).content
    else:
        standalone_query = query

    intent = _detect_intent(query, standalone_query, llm)
    print(f"[DEBUG] query: {query}")
    print(f"[DEBUG] standalone_query: {standalone_query}")
    print(f"[DEBUG] Detected intent: {intent}")

    intent_config = {
        "MARKET_ANALYSIS": (_MARKET_ANALYSIS_PROMPT, DocType.MARKET_ANALYSIS),
        "BANK_LOAN": (_BANK_LOAN_PROMPT, DocType.BANK_LOAN),
        "PROPERTY_DETAIL": (_PROPERTY_DETAIL_PROMPT, DocType.POST),
    }

    selected_prompt, source = intent_config.get(
        intent, (_CONSULTATION_PROMPT, DocType.POST)
    )
    pinecone_filter = {"source": {"$eq": source}}

    if intent != "BANK_LOAN":
        pinecone_filter = extract_filters(pinecone_filter, standalone_query)

    print(f"[DEBUG] Pinecone filter: {pinecone_filter}")

    raw_results = hybrid_search(
        query=standalone_query,
        pinecone_filter=pinecone_filter,
        vector_top_k=20,
        final_top_k=10,
    )
    print(f"[DEBUG] Raw results count: {len(raw_results)}")

    reranked_docs = rerank_documents(query=standalone_query, documents=raw_results, top_n=k)
    print(f"[DEBUG] Reranked docs count: {len(reranked_docs)}")

    docs = []
    enriched_texts = []

    for i, item in enumerate(reranked_docs, 1):
        text = item.get("text", "")
        metadata = item.get("metadata", {})

        # User explicitly specified: id is taken from post's metadata
        post_id = metadata.get("id", "UNKNOWN")

        page_content = f"[Mã BĐS: {post_id}]\n{text}"
        print(f"\n===== Document {i} =====")
        print(page_content.strip()[:200])

        docs.append(Document(page_content=page_content))
        enriched_texts.append({
            "id": post_id,
            "text": text,
            "metadata": metadata,
            "rrf_score": item.get("rrf_score", 0.0)
        })

    print(f"[DEBUG] Docs passed to LLM: {len(docs)}")

    if not docs:
        docs.append(Document(
            page_content=(
                "⚠️ KHÔNG CÓ DỮ LIỆU — Hệ thống không tìm thấy BĐS nào phù hợp với tiêu chí tìm kiếm.\n"
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
        query_eval = price_evaluator.evaluate(standalone_query)

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
        top_doc_text = docs[0].page_content
        prop_eval = price_evaluator.evaluate(top_doc_text)

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

    # 6. Format context into a single string for the prompt
    context_str = "\n\n".join([doc.page_content for doc in docs])

    qa_prompt = ChatPromptTemplate.from_messages([
        ("system", selected_prompt),
        MessagesPlaceholder("chat_history"),
        ("human", "Lịch sử ngữ cảnh:\n{context}\n\nCâu hỏi: {input}")
    ])

    chain = qa_prompt | llm

    try:
        response = chain.invoke({
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
            "answer": parsed_answer,
            "context": enriched_texts,
        }

    except Exception as e:
        print(f"[rag_qa] Error: {e}")
        return {
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
