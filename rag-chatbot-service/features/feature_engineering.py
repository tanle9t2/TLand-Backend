from pathlib import Path
import joblib
from sklearn.neighbors import KDTree, BallTree
import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from config.setting import HCM_CENTER_LAT, HCM_CENTER_LNG, MODEL_DIR
from utils.helper import get_project_root

MODEL_PATH = Path(get_project_root()) / MODEL_DIR


def handle_missing_data(df):
    """Handle missing values in the dataset"""
    df["has_furniture_info"] = df["furniture_state"].notna().astype(int)
    df["has_legal_info"] = df["legal_status"].notna().astype(int)
    df["furniture_state"] = df["furniture_state"].fillna("Unknown")
    df["legal_status"] = df["legal_status"].fillna("Unknown")
    df["frontage_missing"] = df["frontage"].isna().astype(int)
    df["access_road_missing"] = df["access_road"].isna().astype(int)

    # Fill missing numerical values
    df["frontage"] = df["frontage"].fillna(df["frontage"].median())
    df["access_road"] = df["access_road"].fillna(df["access_road"].median())

    return df


def add_basic_engineered_features(df):
    """Add basic engineered features"""
    df["bathroom_bedroom_ratio"] = df["bathrooms"] / (df["bedrooms"] + 1)
    df["used_area"] = df["area"] * (df["floors"] + 1)
    df["total_rooms"] = df["bedrooms"] + df["bathrooms"]
    df["area_per_room"] = df["area"] / (df["total_rooms"] + 1)
    df["log_area"] = np.log1p(df["area"])
    df["log_used_area"] = np.log1p(df["used_area"])
    df["area_x_bedrooms"] = df["area"] * df["bedrooms"]
    df["floor_x_area"] = df["floors"] * df["area"]

    return df


def add_geographic_features(df, k_neighbors=10):
    """Add geographic features based on k-nearest neighbors"""
    coords = df[["lat", "lng"]].values
    tree = KDTree(coords)

    distances, indices = tree.query(coords, k=min(k_neighbors + 1, len(df)))

    # Exclude self (first neighbor)
    if "price" in df.columns:
        prices = df["price"].values

        neighbor_prices = []
        for row in indices:
            neighbor_idx = row[1:]  # Exclude self
            if len(neighbor_idx) > 0:
                neighbor_prices.append(prices[neighbor_idx])
            else:
                neighbor_prices.append([prices[row[0]]])  # Use self if no neighbors

        neighbor_prices = np.array([np.array(x) for x in neighbor_prices], dtype=object)

        df["knn_price_mean"] = [np.mean(x) for x in neighbor_prices]
        df["knn_price_std"] = [np.std(x) if len(x) > 1 else 0 for x in neighbor_prices]
        df["knn_price_max"] = [np.max(x) for x in neighbor_prices]
        df["knn_price_min"] = [np.min(x) for x in neighbor_prices]

    # Average distance to k nearest neighbors (density indicator)
    df["neighbor_density"] = distances[:, 1:].mean(axis=1)
    density_q75 = df["neighbor_density"].quantile(0.75)
    df["is_isolated"] = (df["neighbor_density"] > density_q75).astype(int)

    return df


def distance_to_center(df):
    """Calculate haversine distance to city center"""
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


def frontage_ratio(df):
    """Calculate frontage to area ratio"""
    df["frontage_area_ratio"] = df["frontage"] / (df["area"] + 1)
    return df


def add_location_cluster(df, k=20, save_model=False):
    """Create location clusters for training"""
    kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
    df["zone_id"] = kmeans.fit_predict(df[["lat", "lng"]])

    if save_model:
        joblib.dump(kmeans, MODEL_PATH / 'kmeans_zone.pkl')

    return df


def add_location_cluster_predict(df):
    """Apply location clusters for prediction"""
    kmeans = joblib.load(MODEL_PATH / 'kmeans_zone.pkl')
    df["zone_id"] = kmeans.predict(df[["lat", "lng"]])
    return df


def add_multi_scale_clusters(df, mode="train"):
    """Add multiple granularity clusters"""
    cluster_sizes = [5, 10, 20, 50]

    for k in cluster_sizes:
        cluster_col = f"cluster_{k}"
        model_path = MODEL_PATH / f"kmeans_{k}.pkl"
        stats_path = MODEL_PATH / f"{cluster_col}_stats.pkl"

        if mode == "train":
            kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
            df[cluster_col] = kmeans.fit_predict(df[["lat", "lng"]])
            joblib.dump(kmeans, model_path)

            # Calculate cluster statistics
            if "price" in df.columns:
                cluster_stats = df.groupby(cluster_col)["price"].agg(["mean", "median", "std"]).fillna(0)
                df[f"{cluster_col}_price_mean"] = df[cluster_col].map(cluster_stats["mean"])
                df[f"{cluster_col}_price_median"] = df[cluster_col].map(cluster_stats["median"])
                df[f"{cluster_col}_price_std"] = df[cluster_col].map(cluster_stats["std"])

                joblib.dump(cluster_stats, stats_path)
        else:
            kmeans = joblib.load(model_path)
            df[cluster_col] = kmeans.predict(df[["lat", "lng"]])

            cluster_stats = joblib.load(stats_path)
            df[f"{cluster_col}_price_mean"] = df[cluster_col].map(cluster_stats["mean"]).fillna(0)
            df[f"{cluster_col}_price_median"] = df[cluster_col].map(cluster_stats["median"]).fillna(0)
            df[f"{cluster_col}_price_std"] = df[cluster_col].map(cluster_stats["std"]).fillna(0)

    return df


def add_zone_price_feature(df, mode="train"):
    """Add zone-based price statistics"""
    zone_price_path = MODEL_PATH / "zone_price.pkl"

    if mode == "train":
        zone_price = df.groupby("zone_id")["price"].agg(["median", "mean", "std"]).fillna(0)
        df["zone_price_median"] = df["zone_id"].map(zone_price["median"])
        df["zone_price_mean"] = df["zone_id"].map(zone_price["mean"])
        df["zone_price_std"] = df["zone_id"].map(zone_price["std"])

        joblib.dump(zone_price, zone_price_path)
    else:
        zone_price = joblib.load(zone_price_path)
        df["zone_price_median"] = df["zone_id"].map(zone_price["median"]).fillna(0)
        df["zone_price_mean"] = df["zone_id"].map(zone_price["mean"]).fillna(0)
        df["zone_price_std"] = df["zone_id"].map(zone_price["std"]).fillna(0)

    # Add zone-area interaction
    df["area_zone_price"] = df["area"] * df["zone_price_mean"]

    return df


def add_neighbor_price(df, radius_km=1.0):
    """Add neighborhood price statistics using BallTree"""
    coords = np.radians(df[["lat", "lng"]].values)
    tree = BallTree(coords, metric="haversine")

    radius = radius_km / 6371.0  # Convert km to radians
    neighbors_idx = tree.query_radius(coords, r=radius)

    neighbor_median = []
    neighbor_mean = []
    neighbor_std = []
    neighbor_count = []

    if "price" in df.columns:
        prices = df["price"].values

        for idx_list in neighbors_idx:
            if len(idx_list) > 1:
                neighbor_prices = prices[idx_list]
                neighbor_median.append(np.median(neighbor_prices))
                neighbor_mean.append(np.mean(neighbor_prices))
                neighbor_std.append(np.std(neighbor_prices))
                neighbor_count.append(len(idx_list))
            else:
                neighbor_median.append(np.nan)
                neighbor_mean.append(np.nan)
                neighbor_std.append(0)
                neighbor_count.append(1)

        df["neighbor_price_median"] = neighbor_median
        df["neighbor_price_mean"] = neighbor_mean
        df["neighbor_price_std"] = neighbor_std
        df["neighbor_count"] = neighbor_count

    return df


def add_price_per_m2(df):
    """Calculate price per square meter (training only)"""
    if "price" in df.columns and "area" in df.columns:
        df["price_per_m2"] = df["price"] / df["area"]
        df["log_price_per_m2"] = np.log1p(df["price_per_m2"])
    return df


def build_features(df, mode="train", save_cluster_model=False):
    """
    Main feature engineering pipeline

    Args:
        df: Input dataframe
        mode: "train" or "predict"
        save_cluster_model: Whether to save clustering models

    Returns:
        DataFrame with engineered features
    """

    df = df.copy()
    df = handle_missing_data(df)

    df = distance_to_center(df)
    df = frontage_ratio(df)

    if mode == "train":
        df = add_location_cluster(df, k=20, save_model=save_cluster_model)
        df = add_multi_scale_clusters(df, mode="train")
        df = add_zone_price_feature(df, mode="train")
    else:
        df = add_location_cluster_predict(df)
        df = add_multi_scale_clusters(df, mode="predict")
        df = add_zone_price_feature(df, mode="predict")

    df = add_basic_engineered_features(df)

    df = add_neighbor_price(df)

    # Fill missing neighbor prices
    if "neighbor_price_median" in df.columns:
        fill_value = (
            df["price"].median()
            if "price" in df.columns
            else df["zone_price_mean"].median()
        )

        df["neighbor_price_median"] = df["neighbor_price_median"].fillna(fill_value)

        if "neighbor_price_mean" in df.columns:
            df["neighbor_price_mean"] = df["neighbor_price_mean"].fillna(fill_value)

        if "neighbor_price_std" in df.columns:
            df["neighbor_price_std"] = df["neighbor_price_std"].fillna(0)

    # Step 6: Geographic features (KNN-based)
    df = add_geographic_features(df, k_neighbors=10)

    if mode == "train":
        df = add_price_per_m2(df)

    return df
