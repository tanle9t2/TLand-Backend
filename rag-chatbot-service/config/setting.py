NUM_FEATURES_ORIGINAL = [
    # Size & structure
    "year",
    "area",
    "bedrooms",
    "bathrooms",
    "floors",
    "lat",
    "lng",

]
NUM_FEATURES_REQUIRED = [
    "area",
    "floors",
    "bathrooms",
    "bedrooms",
    "address",
]

NUM_FEATURES = [
    # Core structure
    "year",
    "log_area",
    "bedrooms",
    "bathrooms",
    "floors",
    "lat",
    "lng",

    # Location strength
    "dist_center",
    "log_dist_center",
    # Zone context
    "zone_price_mean",

    # KNN context
    "knn_price_mean",
    "knn_price_median",
    "knn_price_std",
    "neighbor_density",

    # Interactions
    "area_zone_price",
    "area_x_bedrooms",
    "floor_x_area",
    "floor_density",
    "bathroom_per_floor",
    "area_per_floor",
]

CAT_FEATURES = [
    "legal_status",
    "furniture_state",
    "property_type",
    "property_feature"
]
HCM_KEYWORD = [
    "hồ chí minh",
    "ho chi minh",
    "tp.hcm",
    "tp hcm",
    "tp. hồ chí minh"
]
NUM_COLUMNS = ['address', 'area', 'frontage', 'access_road', 'house_direction',
               'balcony_direction', 'floors', 'bedrooms', 'bathrooms', 'legal_status',
               'furniture_state', 'price', 'year', 'lat', 'lng']
HCM_CENTER_LAT = 10.7769
HCM_CENTER_LNG = 106.7009

MODEL_DIR = "models/"
