from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder, OrdinalEncoder
from config.setting import NUM_FEATURES, CAT_FEATURES


def build_preprocessor():
    return ColumnTransformer([
        ("num", "passthrough", NUM_FEATURES),
        ("cat", OrdinalEncoder(handle_unknown="use_encoded_value", unknown_value=-1), CAT_FEATURES)
    ])
