from pathlib import Path
import numpy as np
import pandas as pd
import joblib
import matplotlib.pyplot as plt

from catboost import CatBoostRegressor
from lightgbm import LGBMRegressor
from sklearn.model_selection import train_test_split, cross_val_score, KFold
from sklearn.pipeline import Pipeline
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error

from features.preprocess import build_preprocessor
from features.feature_engineering import build_features

from config.setting import MODEL_DIR
from utils.helper import get_data_path, get_project_root

df = pd.read_csv(get_data_path("ho_chi_minh_pricing_clean.csv"))

df = build_features(df=df, mode="train", save_cluster_model=True)

df = df[(df.price > df.price.quantile(0.01)) &
        (df.price < df.price.quantile(0.99))]

X = df.drop("price", axis=1)
# y = df["price"]
y = np.log1p(df["price"])

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

model = LGBMRegressor(
    n_estimators=3000,
    learning_rate=0.02,
    num_leaves=80,
    min_child_samples=40,
    subsample=0.8,
    colsample_bytree=0.8,
    reg_alpha=0.5,
    reg_lambda=1.5,
    random_state=42
)

pipe = Pipeline([
    ("prep", build_preprocessor()),
    ("model", model)
])

kfold = KFold(n_splits=5, shuffle=True, random_state=42)

cv_scores = cross_val_score(
    pipe,
    X_train,
    y_train,
    cv=kfold,
    scoring="neg_root_mean_squared_error"
)

pipe.fit(X_train, y_train)

# Predict
y_pred_log = pipe.predict(X_test)

y_test_original = np.expm1(y_test)
y_pred_original = np.expm1(y_pred_log)

r2 = r2_score(y_test_original, y_pred_original)
rmse = np.sqrt(mean_squared_error(y_test_original, y_pred_original))
mae = mean_absolute_error(y_test_original, y_pred_original)

print("CV RMSE:", -cv_scores)
print("Mean CV RMSE:", -cv_scores.mean())

print("MAE:", mae)
print("Test R2:", r2)
print("Test RMSE:", rmse)

model_path = Path(get_project_root()) / MODEL_DIR / "lgbm.pkl"
model_path.parent.mkdir(parents=True, exist_ok=True)

joblib.dump(pipe, model_path)
print("✅ CatBoost model saved")

# Prediction vs Actual
plt.figure()
plt.scatter(y_test_original, y_pred_original)
plt.xlabel("Actual Price")
plt.ylabel("Predicted Price")
plt.title("Prediction vs Actual")
plt.show()

residuals = y_test_original - y_pred_original

plt.figure()
plt.scatter(y_pred_original, residuals)
plt.axhline(y=0)
plt.xlabel("Predicted Price")
plt.ylabel("Residual")
plt.title("Residual Plot")
plt.show()

plt.figure()
plt.plot(-cv_scores)
plt.title("Cross Validation RMSE")
plt.xlabel("Fold")
plt.ylabel("RMSE")
plt.show()

# Price Distribution
plt.figure()
y_test_original.hist(bins=50)
plt.title("Price Distribution")
plt.show()

plt.figure()
plt.plot(range(1, len(cv_scores) + 1), -cv_scores, marker="o")
plt.title("Cross Validation RMSE (log scale)")
plt.xlabel("Fold")
plt.ylabel("RMSE")
plt.show()

# Error vs Area (nếu có column area)
if "area" in X_test.columns:
    plt.figure()
    plt.scatter(X_test["area"], residuals)
    plt.xlabel("Area")
    plt.ylabel("Residual")
    plt.title("Error vs Area")
    plt.show()

model = pipe.named_steps["model"]
preprocessor = pipe.named_steps["prep"]

try:
    feature_names = preprocessor.get_feature_names_out()
    importances = model.feature_importances_

    feat_imp = pd.Series(importances, index=feature_names)
    feat_imp = feat_imp.sort_values(ascending=False)[:15]

    plt.figure()
    feat_imp.plot(kind="barh")
    plt.title("Top Feature Importance (LightGBM)")
    plt.gca().invert_yaxis()
    plt.show()


except Exception as e:
    print("Cannot extract feature importance:", e)
