from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from sklearn.neighbors import KDTree, BallTree
from sklearn.cluster import KMeans

from config.setting import HCM_CENTER_LAT, HCM_CENTER_LNG, MODEL_DIR
from utils.helper import get_project_root

MODEL_PATH = Path(get_project_root()) / MODEL_DIR
MODEL_PATH.mkdir(parents=True, exist_ok=True)


def save_train_reference(df_train):
    ref = df_train[["lat", "lng", "price"]].copy()
    joblib.dump(ref, MODEL_PATH / "train_reference.pkl")


def load_train_reference():
    return joblib.load(MODEL_PATH / "train_reference.pkl")


def add_basic_engineered_features(df):
    df["bathroom_bedroom_ratio"] = df["bathrooms"] / (df["bedrooms"] + 1)
    df["log_area"] = np.log1p(df["area"])

    df["area_x_bedrooms"] = df["area"] * df["bedrooms"]
    df["floor_x_area"] = df["floors"] * df["area"]

    df["floor_density"] = df["floors"] / (df["area"] + 1)

    return df


def distance_to_center(df):
    R = 6371  # km

    lat1 = np.radians(df["lat"])
    lon1 = np.radians(df["lng"])
    lat2 = np.radians(HCM_CENTER_LAT)
    lon2 = np.radians(HCM_CENTER_LNG)

    dlat = lat2 - lat1
    dlon = lon2 - lon1

    a = np.sin(dlat / 2) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2) ** 2
    c = 2 * np.arcsin(np.sqrt(a))

    df["dist_center"] = R * c
    df["log_dist_center"] = np.log1p(df["dist_center"])
    return df


def add_location_cluster(df, mode="train", k=20):
    model_path = MODEL_PATH / "kmeans_zone.pkl"

    if mode == "train":
        kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
        df["zone_id"] = kmeans.fit_predict(df[["lat", "lng"]])
        joblib.dump(kmeans, model_path)
    else:
        kmeans = joblib.load(model_path)
        df["zone_id"] = kmeans.predict(df[["lat", "lng"]])

    return df


def add_zone_price_feature(df, mode="train"):
    zone_price_path = MODEL_PATH / "zone_price.pkl"

    if mode == "train":
        zone_price = df.groupby("zone_id")["price"].mean()
        joblib.dump(zone_price, zone_price_path)
    else:
        zone_price = joblib.load(zone_price_path)

    df["zone_price_mean"] = (
        df["zone_id"]
        .map(zone_price)
        .fillna(zone_price.median())
    )

    df["area_zone_price"] = df["area"] * df["zone_price_mean"]

    return df


def add_knn_price_features(df, k_neighbors=10):
    ref = load_train_reference()

    ref_coords = ref[["lat", "lng"]].values
    ref_prices = ref["price"].values

    tree = KDTree(ref_coords)

    query_coords = df[["lat", "lng"]].values

    distances, indices = tree.query(
        query_coords,
        k=min(k_neighbors, len(ref))
    )

    neighbor_prices = ref_prices[indices]

    df["knn_price_mean"] = neighbor_prices.mean(axis=1)
    df["knn_price_std"] = neighbor_prices.std(axis=1)

    df["neighbor_density"] = distances.mean(axis=1)
    df["neighbor_count"] = indices.shape[1]

    return df


def build_features(df, mode="train"):
    df = df.copy()

    df = distance_to_center(df)
    df = add_location_cluster(df, mode=mode)
    df = add_zone_price_feature(df, mode=mode)
    df = add_basic_engineered_features(df)
    df = add_knn_price_features(df)

    return df
