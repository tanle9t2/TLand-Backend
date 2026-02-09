NUM_FEATURES = [
    # Size & structure
    "area",
    "log_area",
    "total_rooms",
    "bedrooms",
    "bathrooms",
    "floors",
    "bathroom_bedroom_ratio",

    # Road & frontage
    "frontage",
    "access_road",
    "frontage_area_ratio",

    # Location raw
    "lat",
    "lng",
    "dist_center",

    # Location context (rất mạnh)
    "zone_price_mean",
    "neighbor_price_median",
    "neighbor_count",

    # Density / neighborhood
    "neighbor_density",
    "knn_price_mean",
    "knn_price_std",

    # Interactions
    "area_zone_price",
    "area_x_bedrooms",
    "floor_x_area",
]

CAT_FEATURES = [
    "house_direction",
    "balcony_direction",
    "legal_status",
    "furniture_state"
]
HCM_KEYWORD = [
    "hồ chí minh",
    "ho chi minh",
    "tp.hcm",
    "tp hcm",
    "tp. hồ chí minh"
]
HCM_CENTER_LAT = 10.7769
HCM_CENTER_LNG = 106.7009

MODEL_DIR = "models/"
