import numpy as np
import pandas as pd
from features.feature_engineering import build_features

from utils.helper import get_data_path

df = pd.read_csv(get_data_path("ho_chi_minh_pricing_clean.csv"))

df = build_features(df=df, mode="train", save_cluster_model=True)

df["price_per_m2"] = df["price"] / df["area"]

upper = df["price_per_m2"].quantile(0.99)
df = df[df["price_per_m2"] <= upper]
print(df["price"].quantile(0.99))
print(df["price"].describe())

X = df.drop("price", axis=1)
# y = df["price"]
y = np.log1p(df["price"])
