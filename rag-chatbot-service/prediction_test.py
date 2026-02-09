import joblib
from pathlib import Path

from features.feature_engineering import build_features
from utils.helper import get_project_root
from config.setting import MODEL_DIR, NUM_FEATURES, CAT_FEATURES
import pandas as pd

lgbm_model = joblib.load(Path(get_project_root()) / MODEL_DIR / "lgbm.pkl")

ALL_FEATURES = NUM_FEATURES + CAT_FEATURES


def ensure_features(df):
    for col in ALL_FEATURES:
        if col not in df.columns:
            df[col] = None
    return df[ALL_FEATURES]


new_house = pd.DataFrame([{
    "area": 72,
    "house_direction": "Southwest",
    "floors": 3,
    "bedrooms": 3,
    "bathrooms": 4,
    "legal_status": "Have certificate",
    "furniture_state": "Full",
    "lat": 10.810583,
    "lng": 106.709145
}])

new_house = ensure_features(new_house)

new_house = build_features(new_house, mode="predict")

pred_cat = lgbm_model.predict(new_house)

print("Predicted price:", pred_cat)
