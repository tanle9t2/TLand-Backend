import os
from dotenv import load_dotenv
from langchain.schema import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.chains import create_history_aware_retriever, create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_openai import ChatOpenAI
from langchain.schema import Document

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
_CONSULTATION_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("consultation_prompt.txt")
_MARKET_ANALYSIS_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("market_analysis_prompt.txt")
_BANK_LOAN_PROMPT = _BASE_PERSONA + "\n" + _load_prompt("bank_loan_prompt.txt")


def _detect_intent(query: str, standalone_query: str, llm: ChatOpenAI) -> str:
    prompt = ChatPromptTemplate.from_messages([
        ("system", "Phân loại yêu cầu của người dùng vào 1 trong 3 loại: "
                   "'MARKET_ANALYSIS' (hỏi đắt rẻ, so sánh giá, nhận định thị trường BĐS), "
                   "'BANK_LOAN' (hỏi về vay vốn ngân hàng, thiếu tiền, tài chính yếu, hỗ trợ tài chính, lãi suất), "
                   "hoặc 'CONSULTATION' (tìm nhà, hỏi thông tin chung). "
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
            "Bạn là một trợ lý ảo chuyên tổng hợp lại câu/yêu cầu của người dùng dựa trên lịch sử hội thoại.\n\n"
            "NHIỆM VỤ: Đọc hiểu TOÀN BỘ lịch sử hội thoại và câu mới nhất của người dùng. "
            "Sau đó, viết lại nội dung mới nhất thành MỘT CÂU ĐỘC LẬP (standalone query) có chứa ĐẦY ĐỦ ngữ cảnh từ lịch sử (Khu vực, Ngân sách, Loại hình, Diện tích...).\n\n"
            "QUY TẮC BẮT BUỘC:\n"
            "1. CHỈ TRẢ VỀ kết quả duy nhất là CÂU ĐÃ VIẾT LẠI, TUYỆT ĐỐI KHÔNG giải thích, KHÔNG có ngữ đầu ngữ cuối như 'Để tôi tổng hợp...', 'Câu hỏi của bạn là...'.\n"
            "2. Đưa các tiêu chí cụ thể (nếu có từ lịch sử) ghép vào chung với câu hiện tại.\n"
            "3. Nếu BĐS đang được quan tâm có sẵn các thông số kỹ thuật (Diện tích, Số tầng, Phòng ngủ, Phòng tắm...) từ các câu trước, TUYỆT ĐỐI GHI LẠI ĐẦY ĐỦ các thông số này trong câu viết lại. Đừng bỏ sót (Ví dụ: 100m2, 1 tầng, 1 phòng ngủ).\n"
            "4. Nếu câu mới nhất mang tính chất hỏi (VD: hỏi mức giá, tiện ích), hãy ĐẢM BẢO viết lại như một câu hỏi kết hợp tiêu chí (VD: 'Nhà để ở tại Gò Vấp, ngân sách 5 tỷ có mức giá như thế nào?').\n"
            "5. Nếu người dùng thay đổi tiêu chí, hãy dùng giá trị hiện tại phù hợp nhất.\n"
            "6. ĐẶC BIỆT QUAN TRỌNG: Phải BẢO TOÀN YÊU CẦU/Ý ĐỊNH CỐT LÕI của câu mới nhất (ví dụ: than phiền thiếu tiền, hỏi vay vốn ngân hàng, hỏi giá đắt rẻ...). KHÔNG được làm mất ý định này bằng cách tự biến nó thành 1 câu đi tìm nhà đơn thuần.",
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

    # 3. Intent Detection using standalone query
    # Chuyển việc detect intent LÊN TRONG để quyết định chiến lược search
    intent = _detect_intent(query, standalone_query, llm)
    print(f"[DEBUG] query: {query}")
    print(f"[DEBUG] standalone_query: {standalone_query}")
    print(f"[DEBUG] Detected intent: {intent}")

    if intent == "MARKET_ANALYSIS":
        selected_prompt = _MARKET_ANALYSIS_PROMPT
    elif intent == "BANK_LOAN":
        selected_prompt = _BANK_LOAN_PROMPT
    else:
        selected_prompt = _CONSULTATION_PROMPT

    pinecone_filter = None
    if intent != "BANK_LOAN":
        pinecone_filter = extract_filters(standalone_query)

    print(f"[DEBUG] Pinecone filter: {pinecone_filter}")

    raw_results = hybrid_search(
        query=standalone_query,
        pinecone_filter=pinecone_filter,
        vector_top_k=20,
        final_top_k=10,
    )
    print(f"[DEBUG] Raw results count: {len(raw_results)}")

    doc_texts = [r["text"] for r in raw_results]
    reranked_texts = rerank_documents(query=standalone_query, documents=doc_texts, top_n=k)
    print(f"[DEBUG] Reranked texts count: {len(reranked_texts)}")

    for i, text in enumerate(reranked_texts, 1):
        print(f"\n===== Document {i} =====")
        print(text.strip()[:200])
    docs = []

    for text in reranked_texts:
        docs.append(Document(page_content=text))
    enriched_texts = reranked_texts

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

    qa_prompt = ChatPromptTemplate.from_messages([
        ("system", selected_prompt),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])

    question_answer_chain = create_stuff_documents_chain(llm, qa_prompt)

    try:
        response = question_answer_chain.invoke({
            "input": query,
            "chat_history": formatted_history,
            "context": docs,
        })
        print(f"[DEBUG] ans: {response}")

        return {
            "answer": response,
            "context": enriched_texts,
        }

    except Exception as e:
        print(f"[rag_qa] Error: {e}")
        return {
            "answer": "Xin lỗi, tôi gặp sự cố kỹ thuật. Vui lòng thử lại sau.",
            "context": [],
        }


def get_query_vector(query: str):
    from langchain_openai import OpenAIEmbeddings
    embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
    return embedding_model.embed_query(query)


from typing import List
from langchain_core.messages import BaseMessage


def _format_chat_history(chat_history) -> List[BaseMessage]:
    if not chat_history:
        return []
    formatted = []
    for msg in chat_history:
        if isinstance(msg, dict):
            if msg.get("human"):
                formatted.append(HumanMessage(content=msg["human"]))
            if msg.get("ai"):
                formatted.append(AIMessage(content=msg["ai"]))
        elif hasattr(msg, "content"):
            formatted.append(msg)
    return formatted
