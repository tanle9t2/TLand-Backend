import os
import logging
import joblib
import json
import numpy as np
import pandas as pd
from pathlib import Path

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

from crawler.crawler import normalize_furniture, normalize_legal_status
from features.feature_engineering import build_features
from features.geo import get_nearby_amenities, goong_geocode

from utils.helper import get_project_root
from config.setting import MODEL_DIR

load_dotenv()

PROMPT_DIR = Path(__file__).parent.parent / "prompt"
llm_model = os.getenv("LLM_MODEL", "gpt-4o-mini")


def load_prompt(filename: str) -> str:
    with open(PROMPT_DIR / filename, "r", encoding="utf-8") as f:
        return f.read()


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
)

logger = logging.getLogger("house_service")

_PRICE_EXPLANATION_PROMPT = load_prompt("price_explanation_prompt.txt")
model = joblib.load(Path(get_project_root()) / MODEL_DIR / "lgbm.pkl")


class HouseService:

    @staticmethod
    async def predict(data: dict, is_description=False):
        lat, lng = goong_geocode(data["address"])

        if lat is None or lng is None:
            lat, lng = 10.762622, 106.660172

        df = pd.DataFrame([data])
        df = df.drop(columns=["price"], errors="ignore")
        df["lat"] = lat
        df["lng"] = lng

        df["property_feature"] = normalize_furniture(data["property_feature"])
        df["legal_status"] = normalize_legal_status(data["legal_status"])
        df["furniture_state"] = normalize_furniture(data["furniture_state"])

        df = build_features(df, mode="predict")
        print("===== PREDICT HOUSE =====")
        print(df.to_string())
        print("=====================")
        pred_log = model.predict(df)
        pred_price_per_m2 = np.expm1(pred_log)

        total_price_ty = (
                                 pred_price_per_m2[0] * df["area"].iloc[0]
                         ) / 1000

        result = {
            "price_per_m2": float(pred_price_per_m2[0]),
            "total_price_ty": float(total_price_ty)
        }

        if is_description:
            try:
                llm = ChatOpenAI(model=llm_model, temperature=0)
                raw_amenities = get_nearby_amenities(lat=lat, lng=lng, radius=3000)
                response_amenities = {
                    "schools": raw_amenities.get("schools", [])[:3],
                    "hospitals": raw_amenities.get("hospitals", [])[:3],
                }
                other_amenities = {
                    "schools": len(raw_amenities.get("schools", [])),
                    "hospitals": len(raw_amenities.get("hospitals", [])),
                    "parks": len(raw_amenities.get("parks", [])),
                    "gyms": len(raw_amenities.get("gyms", [])),
                }

                prompt = ChatPromptTemplate.from_messages([
                    ("system", _PRICE_EXPLANATION_PROMPT),
                    ("human",
                     "Thông tin BĐS:\n"
                     "- Địa chỉ: {address}\n"
                     "- Diện tích: {area} m²\n"
                     "- Số tầng: {floors}\n"
                     "- Số phòng ngủ: {bedrooms}\n"
                     "- Số WC: {toilets}\n"
                     "- Pháp lý: {legal_status}\n"
                     "- Nội thất: {furniture_state}\n"
                     "- Đặc điểm: {property_feature}\n\n"
                     "- Các tiện ích xung quanh: {other_amenities}\n\n"
                     "Kết quả dự đoán từ mô hình AI:\n"
                     "- Giá/m²: {price_per_m2} triệu/m²\n"
                     "- Tổng giá ước tính: {total_price_ty} tỷ VND"
                     )
                ])
                chain = prompt | llm.bind(response_format={"type": "json_object"})

                response = await chain.ainvoke({
                    "address": data.get("address", ""),
                    "area": data.get("area", ""),
                    "floors": data.get("floors", ""),
                    "bedrooms": data.get("bedrooms", ""),
                    "toilets": data.get("toilets", ""),
                    "legal_status": data.get("legal_status", ""),
                    "furniture_state": data.get("furniture_state", ""),
                    "property_feature": data.get("property_feature", ""),
                    "other_amenities": other_amenities or {},
                    "price_per_m2": round(float(pred_price_per_m2[0]), 2),
                    "total_price_ty": round(float(total_price_ty), 2),
                })

                logger.info("===== LLM RESPONSE =====")
                logger.info(response.content)
                logger.info("========================")

                result["description"] = json.loads(response.content)
                result["amenities"] = response_amenities
            except Exception as e:
                print(f"[DEBUG] Price explanation error: {e}")
                result["description"] = None

        return result
