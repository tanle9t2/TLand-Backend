import pandas as pd
from tqdm import tqdm
import os

from config.setting import HCM_KEYWORD
from features.feature_engineering import build_features
from utils.helper import get_data_path, get_project_root

import requests


def append_csv(df, path):
    if os.path.exists(path):
        df.to_csv(path, mode="a", header=False, index=False, encoding="utf-8-sig")
    else:
        df.to_csv(path, index=False, encoding="utf-8-sig")


def osm_geocode(address):
    url = "https://nominatim.openstreetmap.org/search"
    params = {
        "q": address,
        "format": "json",
        "limit": 1
    }
    headers = {
        "User-Agent": "HousePriceBot/1.0 (fcletan12@gmail.com)"
    }
    try:
        r = requests.get(url, params=params, headers=headers, timeout=5)
        if r.status_code == 200:
            data = r.json()
            if len(data) > 0:
                lat = float(data[0]["lat"])
                lng = float(data[0]["lon"])
                return lat, lng
        else:
            print("Status:", r.status_code)

    except Exception as e:
        print("Error:", e)

    return None, None


def get_geo_data(dataset, cache_path="geocode_cache.csv"):
    if "address" not in dataset.columns:
        raise ValueError("Dataset must have 'address' column")

    if os.path.exists(cache_path):
        cache = pd.read_csv(cache_path).set_index("address")
        print(f"Loaded geocode cache: {len(cache)} rows")
    else:
        cache = pd.DataFrame(columns=["lat", "lng"])
        cache.index.name = "address"

    unique_addresses = dataset["address"].unique()

    lat_map = {}
    lng_map = {}

    new_cache_rows = []

    for addr in tqdm(unique_addresses):
        try:
            if not isinstance(addr, str) or not addr.strip():
                lat, lng = None, None

            elif addr in cache.index:
                lat, lng = cache.loc[addr]

            else:
                lat, lng = osm_geocode(addr + ", Vietnam")
                new_cache_rows.append({
                    "address": addr,
                    "lat": lat,
                    "lng": lng
                })

        except Exception as e:
            print(f"Error with address: {addr} -> {e}")
            lat, lng = None, None
            new_cache_rows.append({
                "address": addr,
                "lat": lat,
                "lng": lng
            })

        print(f"Address: {addr} -> {lat}, {lng}")
        lat_map[addr] = lat
        lng_map[addr] = lng

    dataset["lat"] = dataset["address"].map(lat_map)
    dataset["lng"] = dataset["address"].map(lng_map)

    # append cache mới
    if new_cache_rows:
        append_csv(pd.DataFrame(new_cache_rows), cache_path)

    before = len(dataset)
    dataset = dataset.dropna(subset=["lat", "lng"])
    print(f"Geocode success: {len(dataset)} / {before}")

    return dataset


def filter_hcm_data(dataset):
    dataset.columns = (
        dataset.columns
        .str.strip()
        .str.lower()
        .str.replace(" ", "_")
    )

    if "address" not in dataset.columns:
        raise ValueError("Dataset must have 'address' column")

    dataset["address_lower"] = dataset["address"].str.lower()

    mask = dataset["address_lower"].apply(
        lambda x: any(k in x for k in HCM_KEYWORD)
    )

    dataset_hcm = dataset[mask].copy()
    dataset_hcm.drop(columns=["address_lower"], inplace=True)

    dataset_hcm[["house_direction", "balcony_direction"]] = (
        df[["house_direction", "balcony_direction"]]
        .apply(lambda col: col.str.replace("-", " ", regex=False)
               .str.replace(r"\s+", " ", regex=True)
               .str.strip())
    )
    dataset_hcm["year"] = 2024
    print(f"Keep {len(dataset_hcm)} / {len(dataset)} rows in TP.HCM")
    return dataset_hcm


if __name__ == "__main__":
    input_path = get_data_path("ho_chi_minh_pricing_new.csv")
    # "nhatot.csv"
    # "ho_chi_minh_pricing" "vietnam_housing_dataset"
    df = pd.read_csv(input_path)

    df_hcm = get_geo_data(df)

    output_path = get_data_path("final_data.csv")
    append_csv(df_hcm, output_path)

    print(f"💾 Data appended to: {output_path}")
