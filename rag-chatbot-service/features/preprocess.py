from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, OrdinalEncoder, StandardScaler
from config.setting import NUM_FEATURES, CAT_FEATURES, NUM_FEATURES_ORIGIN


def build_preprocessor():
    numerical_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median"))
        ]
    )

    categorical_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("onehot", OneHotEncoder(
                handle_unknown="ignore",
                sparse_output=False  # 👈 QUAN TRỌNG
            ))
        ]
    )

    preprocessor = ColumnTransformer([
        ("num", numerical_transformer, NUM_FEATURES),
        ("cat", categorical_transformer, CAT_FEATURES)
    ])

    preprocessor.set_output(transform="pandas")

    return preprocessor

# def build_preprocessor():
#     numerical_transformer = Pipeline(
#         steps=[
#             ("imputer", SimpleImputer(strategy="median")),
#             ("scaler", StandardScaler())
#         ]
#     )
#
#     categorical_transformer = Pipeline(
#         steps=[
#             ("imputer", SimpleImputer(strategy="most_frequent")),
#             ("onehot", OneHotEncoder(handle_unknown="ignore"))
#         ]
#     )
#     return ColumnTransformer([
#         ("num", numerical_transformer, NUM_FEATURES),
#         ("cat", categorical_transformer, CAT_FEATURES)
#     ])

# return ColumnTransformer([
#     ("num", "passthrough", NUM_FEATURES),
#     ("cat", OrdinalEncoder(handle_unknown="use_encoded_value", unknown_value=-1), CAT_FEATURES)
# ])
