"""
price_evaluation_service.py
Extracts house features from text using LLM and predicts price using LGBM model.
Compares actual price with predicted price to provide market evaluation.
"""

import json
from typing import Dict, Any, Optional
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

from config.setting import NUM_FEATURES_REQUIRED
from service.house_predict_service import HouseService

_EXTRACTION_PROMPT = """
Trích xuất thông tin bất động sản từ đoạn văn bản sau để đưa vào mô hình dự báo giá.
Trả về kết quả dưới định dạng JSON duy nhất.

Văn bản:
"{text}"

Các trường cần trích xuất (nếu không có hãy để giá trị null):
- area (float): Diện tích đất (m2)
- actual_price (float): Giá bán hoặc giá thuê thực tế được nhắc tới trong bài (VNĐ)
- floors (int): Số tầng
- bedrooms (int): Số phòng ngủ
- bathrooms (int): Số phòng vệ sinh
- property_type (str): Loại BĐS (ví dụ: 'Nhà riêng', 'Căn hộ', 'Đất nền')
- property_feature (str): Đặc điểm (ví dụ: 'Hẻm xe hơi', 'Nội thất cao cấp')
- legal_status (str): Pháp lý (ví dụ: 'Sổ hồng', 'Sổ đỏ')
- furniture_state (str): Tình trạng nội thất
- address (str): Địa chỉ cụ thể
- year (int): Năm trích xuất (mặc định 2026 nếu không được nhắc tới)

JSON Format:
{{
    "area": float or null,
    "actual_price": float or null,
    "floors": int or null,
    "bedrooms": int or null,
    "bathrooms": int or null,
    "property_type": "string" or null,
    "property_feature": "string" or null,
    "legal_status": "string" or null,
    "furniture_state": "string" or null,
    "address": "string" or null,
    "year": 2026
}}
"""


class PriceEvaluationService:
    def __init__(self, model_name: str = "gpt-4o-mini"):
        self.llm = ChatOpenAI(model=model_name, temperature=0)
        self.prompt = ChatPromptTemplate.from_template(_EXTRACTION_PROMPT)

    def evaluate(self, doc_text: str, actual_price: Optional[float] = None) -> Dict[str, Any]:
        """
        1. Extract features from text
        2. Predict price using HouseService
        3. Compare with actual price
        """
        try:

            chain = self.prompt | self.llm
            response = chain.invoke({"text": doc_text})

            content = response.content.strip()
            if content.startswith("```json"):
                content = content[7:-3].strip()

            features = json.loads(content)

            print(f"FEATURE EXTRACT: {features}")
            required_fields = NUM_FEATURES_REQUIRED
            missing_fields = [f for f in required_fields if features.get(f) is None]

            if actual_price is None:
                actual_price = features.get("actual_price")

            # Step 2: Predict price
            predicted_price_ty = 0.0
            prediction_made = False

            if not missing_fields:
                prediction = HouseService.predict(features)
                predicted_price_ty = prediction["total_price_ty"]
                prediction_made = True

            # Step 3: Compare
            diff_percent = 0.0
            if not prediction_made:
                status = "Chưa đủ thông tin để định giá"
            elif actual_price and actual_price > 0:
                actual_ty = actual_price / 1_000_000_000
                diff = actual_ty - predicted_price_ty
                diff_percent = (diff / predicted_price_ty) * 100 if predicted_price_ty > 0 else 0.0

                if diff_percent < -10:
                    status = "Rẻ (Dưới giá thị trường)"
                elif diff_percent > 10:
                    status = "Đắt (Trên giá thị trường)"
                else:
                    status = "Hợp lý (Sát giá thị trường)"
            else:
                status = "Đã dự đoán giá thành công"

            evaluation = {
                "predicted_price_ty": predicted_price_ty,
                "features": features,
                "missing_fields": missing_fields,
                "status": status,
                "diff_percent": diff_percent
            }

            return evaluation

        except Exception as e:
            print(f"[PriceEvaluationService] Error: {e}")
            return {"error": str(e)}


    def evaluate_from_metadata(self, metadata: dict) -> Dict[str, Any]:
        """
        Evaluate price directly from Pinecone structured metadata,
        bypassing the costly LLM feature-extraction step.

        Pinecone metadata field mapping:
          area, floors, bedrooms, bathrooms, property_type,
          property_feature, legal_status, furniture_state,
          address, price (actual price in VND)
        """
        try:
            # Map Pinecone metadata → model feature dict
            features = {
                "area":             metadata.get("area"),
                "actual_price":     metadata.get("price"),
                "floors":           metadata.get("floors"),
                "bedrooms":         metadata.get("bedrooms"),
                "bathrooms":        metadata.get("bathrooms"),
                "property_type":    metadata.get("property_type"),
                "property_feature": metadata.get("property_feature"),
                "legal_status":     metadata.get("legal_status"),
                "furniture_state":  metadata.get("furniture_state"),
                "address":          metadata.get("address"),
                "year":             metadata.get("year", 2026),
            }

            print(f"[PriceEval] evaluate_from_metadata features: {features}")

            required_fields = NUM_FEATURES_REQUIRED
            missing_fields = [f for f in required_fields if features.get(f) is None]

            actual_price = features.get("actual_price")

            predicted_price_ty = 0.0
            prediction_made = False

            if not missing_fields:
                prediction = HouseService.predict(features)
                predicted_price_ty = prediction["total_price_ty"]
                prediction_made = True

            diff_percent = 0.0
            if not prediction_made:
                status = "Chưa đủ thông tin để định giá"
            elif actual_price and actual_price > 0:
                actual_ty = actual_price / 1_000_000_000
                diff = actual_ty - predicted_price_ty
                diff_percent = (diff / predicted_price_ty) * 100 if predicted_price_ty > 0 else 0.0

                if diff_percent < -10:
                    status = "Rẻ (Dưới giá thị trường)"
                elif diff_percent > 10:
                    status = "Đắt (Trên giá thị trường)"
                else:
                    status = "Hợp lý (Sát giá thị trường)"
            else:
                status = "Đã dự đoán giá thành công"

            return {
                "predicted_price_ty": predicted_price_ty,
                "features":           features,
                "missing_fields":     missing_fields,
                "status":             status,
                "diff_percent":       diff_percent,
            }

        except Exception as e:
            print(f"[PriceEvaluationService.from_metadata] Error: {e}")
            return {"error": str(e)}


price_evaluator = PriceEvaluationService()
