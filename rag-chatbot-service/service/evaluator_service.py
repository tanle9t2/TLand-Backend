import os
import pandas as pd
from dotenv import load_dotenv
from ragas import SingleTurnSample
from ragas.llms import LangchainLLMWrapper
from langchain_openai import ChatOpenAI
from ragas.embeddings import OpenAIEmbeddings
from datetime import datetime

import asyncio
from ragas.metrics import (
    Faithfulness,
    AnswerRelevancy,
    ContextRecall,
    ContextPrecision
)

from service.rag_qa import ask_question
import openai

load_dotenv()
openai_client = openai.OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

evaluator_llm = LangchainLLMWrapper(ChatOpenAI(model="gpt-3.5-turbo"))


class FixedOpenAIEmbeddings(OpenAIEmbeddings):
    def embed_query(self, text: str):
        return self.embed_text(text)

    def embed_documents(self, texts):
        """Embed multiple documents/texts"""
        if isinstance(texts, str):
            texts = [texts]
        return [self.embed_text(text) for text in texts]


# Khởi tạo embeddings cho RAGAS
evaluator_embeddings = FixedOpenAIEmbeddings(
    model="text-embedding-3-small",
    client=openai_client
)

faithfulness_metric = Faithfulness(llm=evaluator_llm)

metrics_instances = {
    "faithfulness": Faithfulness(llm=evaluator_llm),
    "answer_relevancy": AnswerRelevancy(llm=evaluator_llm, embeddings=evaluator_embeddings),
    "context_recall": ContextRecall(llm=evaluator_llm),
    "context_precision": ContextPrecision(llm=evaluator_llm),
}


async def evaluate_sample_multi_metric(sample):
    rag_result = ask_question(sample["question"], sample["history"])

    rag_answer = rag_result.get("answer", "")
    rag_contexts = rag_result.get("context", [])

    no_info_case = sample.get("no_info_case", False)

    result = {
        "question": sample["question"],
        "reference": sample.get("expected_answer", ""),
        "rag_answer": rag_answer,
        "context": " | ".join(rag_contexts),
    }

    if no_info_case:
        # Custom evaluation
        if any(kw in rag_answer.lower() for kw in ["không có", "không tìm thấy", "chưa có thông tin"]):
            result["faithfulness_score"] = 1.0
            result["answer_relevancy_score"] = 1.0
        else:
            result["faithfulness_score"] = 0.0
            result["answer_relevancy_score"] = 0.0
        result["context_recall_score"] = None
        result["context_precision_score"] = None
    else:
        # Evaluate with RAGAS normally
        single_sample = SingleTurnSample(
            user_input=sample["question"],
            response=rag_answer,
            retrieved_contexts=rag_contexts,
            reference=sample.get("expected_answer", "")
        )

        for name, metric in metrics_instances.items():
            try:
                score = await metric.single_turn_ascore(sample=single_sample)
                result[name + "_score"] = score
            except Exception as e:
                print(f"Error calculating {name} for question '{sample['question']}': {e}")
                result[name + "_score"] = None

    return result


async def evaluate_batch(samples):
    tasks = [evaluate_sample_multi_metric(sample) for sample in samples]
    return await asyncio.gather(*tasks)


sample_list = [
    {
        "question": "Xu hướng bất động sản hiện tại ở TP.HCM",
        "expected_answer": "Xu hướng bất động sản tại TP.HCM đang dần hồi phục, giá bán đất và chung cư tăng cao, và các rào cản pháp lý đang dần được giải quyết.",
        "history": [],
    },
    {
        "question": "Tui nên đầu tư vào khu vực nào ở TP.HCM",
        "expected_answer": "Nếu bạn muốn đầu tư ở TP.HCM thì hiện có vài hướng đáng cân nhắc:\n\n- Muốn an toàn, dễ cho thuê: Quận 2 (Thủ Đức), Bình Thạnh – gần trung tâm, nhu cầu cao.\n- Muốn tăng giá dài hạn nhờ hạ tầng: Quận 9 (Thủ Đức), Bình Chánh, Nhà Bè – nhiều dự án giao thông và khu đô thị mới.\n- Muốn sinh lời trung hạn: Hóc Môn, Củ Chi – đang được định hướng phát triển thành thành phố vệ tinh.\n\nTùy bạn ưu tiên ngắn hạn hay dài hạn, cho thuê hay mua đi bán lại. Bạn muốn mình gợi ý chi tiết theo nhu cầu nào?",
        "history": [],

    },
    {
        "question": "Tui cần vay khoảng 5 tỷ để mua nhà. Bạn có thể chọn giúp tui một số khoảng vay phù hợp",
        "expected_answer": "Hiện tại có nhiều ngân hàng đang triển khai gói vay mua nhà với lãi suất ưu đãi, bạn có thể tham khảo:\n\n**Nhóm ngân hàng Nhà nước (Big 4):**\n- Agribank: Lãi suất từ 5,5%/năm, hạn mức vay đến 100% nhu cầu vốn nếu có tài sản bảo đảm, thời hạn tối đa 40 năm, ân hạn gốc 5 năm.\n- BIDV: Hỗ trợ vay tối đa 5 tỷ, lãi suất cố định 5,5%/năm trong 3 năm, thời gian vay đến 40 năm, miễn trả gốc 5 năm.\n- VietinBank: Gói HomeJoy cho khách hàng trẻ với lãi suất từ 5,6%/năm, vay 100% nhu cầu vốn, thời gian tối đa 40 năm, ân hạn gốc 5 năm.\n- Vietcombank: Gói 'Nhà Mới Thành Đạt' cho người trẻ, lãi suất từ 5,2%/năm, vay tối đa 70% giá trị nhà, thời hạn 40 năm, ân hạn gốc 5 năm.\n\n**Ngân hàng TMCP tiêu biểu:**\n- Techcombank: Lãi suất từ 3,99%/năm, vay tối đa 80% tài sản bảo đảm, thời hạn 45 năm, miễn trả gốc đến 10 năm.\n- TPBank: Ưu đãi 3,6%/năm trong 3 tháng đầu, ân hạn gốc 60 tháng.\n- VPBank: Lãi suất từ 5,2%/năm, vay đến 80% giá trị nhà, thời hạn 25 năm.\n- Eximbank: Gói 'Vững tổ ấm – Chắc tương lai' với lãi suất chỉ từ 3,68%/năm, vay đến 40 năm, ân hạn gốc 7 năm.\n\n👉 Với khoản vay 5 tỷ, bạn có thể cân nhắc Big 4 để an toàn và ổn định, hoặc các ngân hàng TMCP như Techcombank, Eximbank nếu muốn lãi suất thấp ban đầu và thời hạn vay dài. Bạn muốn mình so sánh chi tiết hơn theo tiêu chí nào (lãi suất thấp nhất, vay dài hạn nhất hay thủ tục nhanh nhất)?",
        "history": [],
    },
    {
        "question": "Tui muốn mua nhà 10 tỷ ở Vĩnh Long có không?",
        "expected_answer": "Dựa trên dữ liệu hiện có, tôi không tìm thấy bất động sản nào phù hợp với yêu cầu này. Bạn có muốn tôi gợi ý một số phân khúc khác hoặc khu vực lân cận không?",
        "no_info_case": True,
        "history": [],
    },
    {
        "question": "Giới thiệu cho tui các bất động sản ở Gò Vấp giá khoảng 2 tỷ",
        "expected_answer": "Hiện tại, ở Gò Vấp không có bất động sản nào với giá khoảng 2 tỷ như mong muốn của bạn. Bạn có muốn tui giới thiệu cho bạn tại những khu vực khác hoặc bạn có thể tăng ngân sách .",
        "history": [],
    },
    {
        "question": "Tui cần thuê nhà trọ ở Bình Thạnh gần các trường đại học",
        "expected_answer": "🏠 Phòng trọ cao cấp dạng Studio Full NT - Trung tâm Q. Bình Thạnh, 🌸CĂN HỘ DUPLEX FULL NỘI THẤT-35M2 -NGAY ĐH HTECH,GTVT,-Ở ĐƯỢC 4 NGƯỜI ",
        "history": [],
    },
    {
        "question": "Hiện tại nếu mua nhà ở gò vấp với giá 2 tỷ đầy đủ nội thất là mắc hay rẻ?",
        "expected_answer": " chúng ta cần xem xét một số yếu tố sau: Thị trường hiện tại.Giá trung bình căn hộ tại Gò Vấp hiện nay đang dao động từ 2-5 tỷ đồng.  Tuy nhiên cần chính xác vị trí và các thông tin khác để xác định cụ thể. ",
        "history": [],
    },
    {
        "question": "Khoảng 30 tỷ",
        "expected_answer": "[Biệt thự Nguyễn Oanh, Gò Vấp], địa chỉ Đường Nguyễn Oanh, Gò Vấp, Hồ Chí Minh.Đặc điểm: Biệt thự cao cấp, nội thất sang trọng, view đẹp",
        "history": [
            {
                "human": "Tui muốn mua nhà ở Nguyễn Oanh, GV",
                "ai": "Dựa trên dữ liệu hiện có, tôi không tìm thấy BĐS phù hợp với yêu cầu của bạn. \n\nĐể tư vấn chính xác nhất, bạn có thể cho mình biết thêm về ngân sách cụ thể, khu vực ưu tiên, hoặc mục đích sử dụng của bạn không?"
            }
        ]
    },
    {
        "question": "Cho tui một chiến lược đầu tư lâu dài",
        "expected_answer": "Chọn các dự án có vị thế mạnh trên thị trường và được đánh giá cao về thương hiệu. Đầu tư vào các dự án có quỹ đất sạch. Tập trung vào việc phát triển các dự án liên kết với các đối tác nước ngoài",
        "history": [],
    }

]
if __name__ == "__main__":
    all_results = asyncio.run(evaluate_batch(sample_list))
    df = pd.DataFrame(all_results)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"ragas_metrics_report_{timestamp}.csv"

    df.to_csv(f"../evaluator_reports/{filename}", index=False, encoding="utf-8-sig")
