from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OrdinalEncoder

from config.setting import NUM_FEATURES, CAT_FEATURES


def build_preprocessor():
    numerical_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median"))
        ]
    )

    categorical_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("ordinal", OrdinalEncoder(
                handle_unknown="use_encoded_value",
                unknown_value=-1
            ))
        ]
    )

    preprocessor = ColumnTransformer([
        ("num", numerical_transformer, NUM_FEATURES),
        ("cat", categorical_transformer, CAT_FEATURES)
    ])

    preprocessor.set_output(transform="pandas")

    return preprocessor
