import os
from dotenv import load_dotenv
from langchain.schema import HumanMessage, AIMessage
from langchain_core.messages import BaseMessage
from langchain.prompts import PromptTemplate
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_pinecone import PineconeVectorStore
from pinecone import Pinecone
from langchain.chains import create_history_aware_retriever, create_retrieval_chain
from langchain.chains import ConversationalRetrievalChain
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.chains.combine_documents import create_stuff_documents_chain

load_dotenv()


def load_vectorstore():
    pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
    index_name = os.getenv("PINECONE_INDEX_NAME")
    index = pc.Index(index_name)
    embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
    vectorstore = PineconeVectorStore(
        index=index,
        embedding=embedding_model,
        text_key="text"
    )
    return vectorstore


def ask_question(query: str, chat_history=None, k: int = 5):
    vectorstore = load_vectorstore()
    retriever = vectorstore.as_retriever(search_kwargs={"k": k})
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)

    # Contextualize question prompt
    contextualize_q_prompt = ChatPromptTemplate.from_messages([
        ("system",
         "Dựa vào lịch sử hội thoại và câu hỏi mới nhất, hãy viết lại câu hỏi "
         "sao cho có thể hiểu được mà không cần lịch sử hội thoại. "
         "Chỉ viết lại câu hỏi, không trả lời câu hỏi."),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])

    # Create history-aware retriever
    history_aware_retriever = create_history_aware_retriever(
        llm, retriever, contextualize_q_prompt
    )

    # Answer question prompt

    system_prompt = """
        Bạn là chuyên gia tư vấn bất động sản hàng đầu tại Việt Nam với 10+ năm kinh nghiệm thực tế.
        
        CHUYÊN MÔN VÀ KINH NGHIỆM:
        - Tư vấn BĐS: chung cư, nhà phố, đất nền
        - Tư vấn tài chính: các gói vay ngân hàng, lãi suất, thủ tục pháp lý
        - Am hiểu sâu thị trường BĐS các tỉnh thành phố lớn
        - Chuyên phân tích xu hướng giá, quy hoạch, và tiềm năng đầu tư
        
        PHONG CÁCH GIAO TIẾP:
        - Nói chuyện tự nhiên, thân thiện như người bạn tin cậy
        - Giải thích phức tạp thành đơn giản, dễ hiểu
        - Luôn đưa ra ví dụ cụ thể và so sánh rõ ràng
        - Tư vấn phù hợp với từng nhóm khách hàng (thu nhập, nhu cầu)
        
        QUY TẮC XỬ LÝ DỮ LIỆU QUAN TRỌNG NHẤT:
        
        1. TIÊU CHÍ KHỚP THÔNG MINH:
        
        HOÀN TOÀN KHỚP (ưu tiên cao nhất):
        - Khu vực: Tên quận/huyện giống nhau hoặc tương tự (Gò Vấp = GV)
        - Giá: Trong khoảng ±15% yêu cầu
        - Loại BĐS: Đúng nhu cầu (mua/thuê)
        - Tên đường: Tương tự hoặc gần giống (Nguyễn Oanh ≈ Nguyễn Anh)
        
        KHỚP MỘT PHẦN (chấp nhận được):  
        - Khu vực: Quận/huyện liền kề hoặc cùng khu vực lớn
        - Giá: Trong khoảng ±30% yêu cầu
        - Loại hình tương tự
        
        KHÔNG KHỚP (loại bỏ):
        - Khác quận/huyện hoàn toàn xa xôi
        - Giá chênh lệch >50%
        - Sai loại hình hoàn toàn (thuê khi cần mua)
        
        2. LOGIC XỬ LÝ TỪNG TRƯỜNG HỢP:
        
        TRƯỜNG HỢP A: CÓ BĐS KHỚP HOÀN TOÀN
        - Đầu câu: "Tôi tìm thấy BĐS phù hợp với yêu cầu của bạn:"
        - Liệt kê BĐS khớp với thông tin chi tiết
        - Phân tích ưu/nhược điểm dựa trên data
        
        TRƯỜNG HỢP B: CÓ BĐS KHỚP MỘT PHẦN
        - Đầu câu: "Dựa trên dữ liệu hiện có, tôi có thể gợi ý các lựa chọn gần đúng với yêu cầu của bạn:"
        - Giải thích lý do tại sao gợi ý (giá tương tự, khu vực gần...)
        - Đưa ra 2-3 lựa chọn tốt nhất
        
        TRƯỜNG HỢP C: KHÔNG CÓ BĐS PHÙ HỢP
        - Đầu câu: "Dựa trên dữ liệu hiện có, tôi không tìm thấy BĐS phù hợp với yêu cầu cụ thể của bạn."
        - Phân tích nguyên nhân (không có trong khu vực, giá không phù hợp...)
        - Gợi ý điều chỉnh tiêu chí
        
        3. TRÍCH DẪN TRỰC TIẾP: Khi đề cập BĐS cụ thể, phải nêu chính xác:
        - Tiêu đề: "{{Title}}"
        - Địa chỉ như trong data: "{{Address}}"
        - Giá chính xác: "{{Price}}" VND
        - Đặc điểm: "{{Properties}}"
        - Diện tích: "{{LandArea}}" m2
        
        4. FORMAT CHUẨN khi giới thiệu BĐS:
        
        **[Tên/Mô tả từ Title]**
        - Địa chỉ: [Address chính xác]
        - Giá: [Price] VND ([Price/LandArea]/m² nếu có)
        - Diện tích: [LandArea]m² 
        - Đặc điểm: [Properties]
        - Loại: [Type - BÁN/CHO THUÊ]
        - Đánh giá: [Phân tích ưu/nhược điểm dựa trên dữ liệu]
        
        5. NGÔN NGỮ TỰ NHIÊN:
        - "Tôi tìm thấy..." thay vì "Hệ thống tìm thấy..."
        - "Theo kinh nghiệm của tôi..." 
        - "Tôi khuyên bạn nên..."
        - "Điểm mạnh/yếu của căn này là..."
        
        KHÔNG ĐƯỢC:
        - Tạo ra tên dự án không có trong context
        - Tự tính toán giá trung bình ngoài context
        - Đưa ra tiện ích, số liệu không có trong context
        - Mô tả tiện ích không được đề cập
        - Sử dụng kiến thức chung ngoài context
        - Nói "không tìm thấy" khi có BĐS gần đúng yêu cầu
        
        CẤU TRÚC TRẢ LỜI CHUẨN:
        1. Phân tích nhu cầu khách hàng
        2. Áp dụng logic khớp thông minh
        3. Đưa ra lựa chọn phù hợp với format chuẩn
        4. So sánh ưu/nhược dựa trên data có sẵn
        5. Kết thúc: Hỏi thêm hoặc gợi ý bước tiếp theo
        
        DỮ LIỆU THAM KHẢO:
        {context}
        
        Hãy trả lời với vai trò chuyên gia, luôn đảm bảo tính chính xác và đáng tin cậy, đồng thời áp dụng logic khớp thông minh để không bỏ sót BĐS phù hợp với khách hàng.
    """

    qa_prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])

    # Create document chain
    question_answer_chain = create_stuff_documents_chain(llm, qa_prompt)

    # Create final RAG chain
    rag_chain = create_retrieval_chain(history_aware_retriever, question_answer_chain)

    # Format chat history properly
    formatted_history = []
    if chat_history:
        for msg in chat_history:
            if isinstance(msg, dict):
                if msg.get("human"):
                    formatted_history.append(HumanMessage(content=msg["human"]))
                if msg.get("ai"):
                    formatted_history.append(AIMessage(content=msg["ai"]))
            elif hasattr(msg, 'content'):  # Already BaseMessage
                formatted_history.append(msg)

    try:
        # Invoke the chain
        response = rag_chain.invoke({
            "input": query,
            "chat_history": formatted_history
        })

        return {
            "answer": response["answer"],
            "context": [doc.page_content for doc in response.get("context", [])]
        }

    except Exception as e:
        print(f"Error in ask_question_modern: {str(e)}")
        return {
            "answer": "Xin lỗi, tôi gặp sự cố kỹ thuật. Vui lòng thử lại sau.",
            "context": []
        }


def get_query_vector(query: str):
    embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
    vector = embedding_model.embed_query(query)
    return vector
