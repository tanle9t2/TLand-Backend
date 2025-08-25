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
    print(index_name)
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
    llm = ChatOpenAI(model="gpt-3.5-turbo", temperature=0)

    # Define prompt template
    prompt = ChatPromptTemplate.from_messages([
        ("system",
         "Bạn là một chuyên gia tư vấn bất động sản. "
         "Nhiệm vụ của bạn là hỗ trợ khách hàng tìm kiếm và lựa chọn bất động sản phù hợp. "
         "Bạn phải dựa trên dữ liệu đã cung cấp. "
         "Nếu dữ liệu không đủ để trả lời, hãy hỏi thêm thông tin cụ thể từ khách hàng. "
         "Nếu sau khi hỏi thêm mà vẫn không có dữ liệu phù hợp, hãy lịch sự nói rằng bạn không thể trả lời."),

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
