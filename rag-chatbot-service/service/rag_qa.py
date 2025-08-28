import os
from dotenv import load_dotenv
from langchain.schema import HumanMessage, AIMessage
from langchain_core.messages import BaseMessage

from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_pinecone import PineconeVectorStore
from pinecone import Pinecone

from langchain.chains import ConversationalRetrievalChain
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

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


def ask_question(query: str, chat_history=None, k: int = 10):
    vectorstore = load_vectorstore()
    retriever = vectorstore.as_retriever(search_kwargs={"k": k})
    llm = ChatOpenAI(model="gpt-3.5-turbo", temperature=0)

    prompt = ChatPromptTemplate.from_messages([
        ("system",
         "Bạn là một chuyên gia tư vấn bất động sản giàu kinh nghiệm tại Việt Nam. "
         "Nhiệm vụ của bạn là: "
         "- Hỗ trợ khách hàng tìm kiếm, lựa chọn bất động sản phù hợp (dựa trên dữ liệu đã cung cấp). "
         "- Tư vấn các khoản vay và giải pháp tài chính khi khách hàng có nhu cầu. "
         "Nguyên tắc: "
        "1. Chỉ sử dụng dữ liệu được cung cấp từ ngữ cảnh để trả lời. Không cần nói rõ nguồn dữ liệu hay nhắc 'dựa vào dữ liệu đã cung cấp'. Hãy trả lời trực tiếp, tự nhiên, như đang nói chuyện với khách hàng."
         "2. Nếu dữ liệu không đủ, hãy lịch sự hỏi thêm thông tin cụ thể (ví dụ: khu vực, ngân sách, loại bất động sản). "
         "3. Nếu sau khi hỏi thêm vẫn không có dữ liệu phù hợp, hãy nói rõ ràng và lịch sự rằng bạn không thể trả lời. "
         "4. Trả lời tự nhiên, như một chuyên gia đang tư vấn trực tiếp cho khách hàng. "
         "5. Chỉ tập trung vào lĩnh vực bất động sản và tư vấn tài chính liên quan, không trả lời ngoài phạm vi."
         "6. Nếu người dùng hỏi ngoài phạm vi (ví dụ: lập trình, toán học, nấu ăn...), hãy lịch sự từ chối và nói:  Xin lỗi, tôi chỉ có thể tư vấn về bất động sản và tài chính liên quan."),


        MessagesPlaceholder(variable_name="history"),

        ("human",
         "Dữ liệu:\n{context}\n\n"
         "Câu hỏi: {question}")
    ])

    formatted_history: list[BaseMessage] = []
    if chat_history:
        for msg in chat_history:
            if isinstance(msg, dict):
                formatted_history.append(HumanMessage(content=msg["human"]))
                formatted_history.append(AIMessage(content=msg["ai"]))
            elif isinstance(msg, BaseMessage):
                formatted_history.append(msg)  # Already valid

    # Set up the chain
    qa_chain = ConversationalRetrievalChain.from_llm(
        llm=llm,
        retriever=retriever,
        combine_docs_chain_kwargs={"prompt": prompt},
        return_source_documents=True
    )
    result = qa_chain.invoke({
        "question": query,
        "chat_history": formatted_history,  # used for context
        "history": formatted_history  # used for prompt rendering
    })
    return {
        "answer": result["answer"],
        "context": [doc.page_content for doc in result["source_documents"]]
    }


def get_query_vector(query: str):
    embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
    vector = embedding_model.embed_query(query)
    return vector
