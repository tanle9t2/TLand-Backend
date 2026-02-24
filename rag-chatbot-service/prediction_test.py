import joblib
from pathlib import Path

import numpy as np

from features.feature_engineering import build_features
from utils.helper import get_project_root
from config.setting import MODEL_DIR, NUM_FEATURES, CAT_FEATURES
import pandas as pd

lgbm_model = joblib.load(Path(get_project_root()) / MODEL_DIR / "catboost.pkl")

ALL_FEATURES = NUM_FEATURES + CAT_FEATURES


def ensure_features(df):
    for col in ALL_FEATURES:
        if col not in df.columns:
            df[col] = None
    return df[ALL_FEATURES]


new_house = pd.DataFrame([{
    "area": 37,
    "floors": 1,
    "bedrooms": 2,
    "bathrooms": 2,
    "property_type": "Nhà ngõ, hẻm",
    "property_feature": "Unknown",
    "legal_status": "Have certificate",
    "furniture_state": None,
    "lat": 10.8413808,
    "lng": 106.7833423,
    "year": 2026
}])

new_house = build_features(new_house, mode="predict")
# new_house = ensure_features(new_house)
pred_log = lgbm_model.predict(new_house)
pred_price_per_m2 = np.expm1(pred_log)

total_price_ty = (pred_price_per_m2[0] * new_house["area"].iloc[0]) / 1000

print(f"Predict price: {total_price_ty:.2f} tỷ")
print(f"Predict price per m2: {pred_price_per_m2[0]:.2f} triệu")
