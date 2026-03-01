import joblib
import numpy as np
import pandas as pd
from pathlib import Path

from crawler.crawler import normalize_furniture, normalize_legal_status
from features.feature_engineering import build_features
from features.geo import osm_geocode
from utils.helper import get_project_root
from config.setting import MODEL_DIR

model = joblib.load(Path(get_project_root()) / MODEL_DIR / "lgbm.pkl")


class HouseService:

    @staticmethod
    def predict(data: dict):
        lat, lng = osm_geocode(data["address"] + ", Vietnam")
        df = pd.DataFrame([data])
        df["lat"] = lat,
        df["lng"] = lng
    
        df["property_feature"] = normalize_furniture(data["property_feature"])
        df["legal_status"] = normalize_legal_status(data["legal_status"])

        df = build_features(df, mode="predict")

        pred_log = model.predict(df)
        pred_price_per_m2 = np.expm1(pred_log)

        total_price_ty = (
                                 pred_price_per_m2[0] * df["area"].iloc[0]
                         ) / 1000

        return {
            "price_per_m2": float(pred_price_per_m2[0]),
            "total_price_ty": float(total_price_ty)
        }
