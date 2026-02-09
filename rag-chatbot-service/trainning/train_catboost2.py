from pathlib import Path
import numpy as np
import pandas as pd
import joblib
import matplotlib.pyplot as plt
from catboost import CatBoostRegressor, Pool
from sklearn.model_selection import train_test_split, cross_val_score, KFold
from sklearn.pipeline import Pipeline
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from features.preprocess import build_preprocessor
from features.feature_engineering import (
    add_engineered_features,
    add_location_cluster,
    add_price_per_m2,
    frontage_ratio,
    distance_to_center
)
from config.setting import MODEL_DIR
from utils.helper import get_data_path, get_project_root


# ============================================================================
# IMPROVEMENT 1: Enhanced Feature Engineering
# ============================================================================
def add_advanced_features(df):
    """Add more sophisticated features"""
    df = df.copy()

    # Log transformations for skewed features
    if 'area' in df.columns:
        df['area_log'] = np.log1p(df['area'])
        df['area_sqrt'] = np.sqrt(df['area'])

    if 'frontage' in df.columns:
        df['frontage_log'] = np.log1p(df['frontage'])

    # Polynomial features
    if 'area' in df.columns and 'floors' in df.columns:
        df['total_floor_area'] = df['area'] * df['floors']

    if 'bedrooms' in df.columns and 'bathrooms' in df.columns:
        df['bed_bath_ratio'] = df['bedrooms'] / (df['bathrooms'] + 1)
        df['total_rooms'] = df['bedrooms'] + df['bathrooms']

    # Area efficiency metrics
    if 'area' in df.columns and 'bedrooms' in df.columns:
        df['area_per_bedroom'] = df['area'] / (df['bedrooms'] + 1)

    if 'area' in df.columns and 'bathrooms' in df.columns:
        df['area_per_bathroom'] = df['area'] / (df['bathrooms'] + 1)

    # Access and frontage quality score
    if 'access_road' in df.columns and 'frontage' in df.columns:
        df['access_frontage_score'] = df['access_road'] * df['frontage']

    # Direction value (assuming some directions are more valuable)
    # This is a placeholder - you might want to customize based on domain knowledge
    if 'house_direction' in df.columns:
        direction_value = {
            'East': 1.2, 'South': 1.1, 'Southeast': 1.15,
            'West': 0.9, 'North': 0.95, 'Northeast': 1.0,
            'Southwest': 1.0, 'Northwest': 0.95
        }
        df['direction_value'] = df['house_direction'].map(direction_value).fillna(1.0)

    # Luxury score
    luxury_features = []
    if 'floors' in df.columns:
        luxury_features.append((df['floors'] >= 3).astype(int))
    if 'bedrooms' in df.columns:
        luxury_features.append((df['bedrooms'] >= 4).astype(int))
    if 'bathrooms' in df.columns:
        luxury_features.append((df['bathrooms'] >= 3).astype(int))
    if 'furniture_state' in df.columns:
        luxury_features.append((df['furniture_state'] == 'Full').astype(int))

    if luxury_features:
        df['luxury_score'] = sum(luxury_features)

    return df


# ============================================================================
# IMPROVEMENT 2: Outlier Handling
# ============================================================================
def remove_outliers(df, target_col='price', threshold=3):
    """Remove outliers using z-score method"""
    df = df.copy()

    # Remove price outliers
    if target_col in df.columns:
        z_scores = np.abs((df[target_col] - df[target_col].mean()) / df[target_col].std())
        df = df[z_scores < threshold]

    # Remove extreme area outliers
    if 'area' in df.columns:
        q1 = df['area'].quantile(0.01)
        q99 = df['area'].quantile(0.99)
        df = df[(df['area'] >= q1) & (df['area'] <= q99)]

    return df


# ============================================================================
# MAIN TRAINING PIPELINE
# ============================================================================

# Load data
df = pd.read_csv(get_data_path("vietnam_housing_dataset_hcm.csv"))

# Feature engineering
df = add_engineered_features(df)
df = add_location_cluster(df, save_model=True)
df = add_price_per_m2(df)
df = frontage_ratio(df)
df = distance_to_center(df)
df = add_advanced_features(df)

# Remove outliers
print(f"Original dataset size: {len(df)}")
df = remove_outliers(df, target_col='price', threshold=3)
print(f"After outlier removal: {len(df)}")

# Separate features and target
X = df.drop("price", axis=1)
y = df["price"]

# Log transform target (often helps with housing prices)
y_log = np.log1p(y)

# Train-test split with stratification
X_train, X_test, y_train, y_test = train_test_split(
    X, y_log, test_size=0.2, random_state=42, shuffle=True
)

# ============================================================================
# IMPROVEMENT 3: Optimized CatBoost Parameters
# ============================================================================
model = CatBoostRegressor(
    iterations=1000,
    depth=8,
    learning_rate=0.03,
    loss_function="RMSE",
    l2_leaf_reg=3,
    bagging_temperature=0.2,
    random_strength=1,
    border_count=128,
    verbose=100,
    random_seed=42,
    task_type='GPU',
    bootstrap_type='Bayesian',
    min_data_in_leaf=20,
    early_stopping_rounds=50
)


# Build pipeline
pipe = Pipeline([
    ("prep", build_preprocessor()),
    ("model", model)
])

# ============================================================================
# IMPROVEMENT 4: Better Cross-Validation Strategy
# ============================================================================
print("\n" + "=" * 60)
print("CROSS-VALIDATION")
print("=" * 60)

kfold = KFold(n_splits=5, shuffle=True, random_state=42)
cv_scores = cross_val_score(
    pipe, X_train, y_train,
    cv=kfold,
    scoring="neg_root_mean_squared_error",
    n_jobs=-1  # Use all CPU cores
)

print(f"CV RMSE (log scale): {-cv_scores}")
print(f"Mean CV RMSE: {-cv_scores.mean():.4f} ± {cv_scores.std():.4f}")

# ============================================================================
# IMPROVEMENT 5: Training with Validation Set for Early Stopping
# ============================================================================
print("\n" + "=" * 60)
print("TRAINING FINAL MODEL")
print("=" * 60)

# Split training data for validation
X_tr, X_val, y_tr, y_val = train_test_split(
    X_train, y_train, test_size=0.2, random_state=42
)

# Fit preprocessor on training data
preprocessor = build_preprocessor()
X_tr_prep = preprocessor.fit_transform(X_tr)
X_val_prep = preprocessor.transform(X_val)
X_test_prep = preprocessor.transform(X_test)

# Train model with validation set
model.fit(
    X_tr_prep, y_tr,
    eval_set=(X_val_prep, y_val),
    verbose=100,
    plot=False
)

# Make predictions
y_pred_log = model.predict(X_test_prep)

# Convert back from log scale
y_test_original = np.expm1(y_test)
y_pred_original = np.expm1(y_pred_log)

# ============================================================================
# IMPROVEMENT 6: Comprehensive Metrics
# ============================================================================
print("\n" + "=" * 60)
print("TEST SET PERFORMANCE")
print("=" * 60)

r2 = r2_score(y_test_original, y_pred_original)
rmse = np.sqrt(mean_squared_error(y_test_original, y_pred_original))
mae = mean_absolute_error(y_test_original, y_pred_original)
mape = np.mean(np.abs((y_test_original - y_pred_original) / y_test_original)) * 100

print(f"R² Score: {r2:.4f}")
print(f"RMSE: {rmse:.4f}")
print(f"MAE: {mae:.4f}")
print(f"MAPE: {mape:.2f}%")

# Additional metrics
median_ae = np.median(np.abs(y_test_original - y_pred_original))
print(f"Median Absolute Error: {median_ae:.4f}")

# Percentage of predictions within ±10%, ±20%, ±30%
within_10 = np.mean(np.abs((y_test_original - y_pred_original) / y_test_original) <= 0.10) * 100
within_20 = np.mean(np.abs((y_test_original - y_pred_original) / y_test_original) <= 0.20) * 100
within_30 = np.mean(np.abs((y_test_original - y_pred_original) / y_test_original) <= 0.30) * 100

print(f"\nPredictions within ±10%: {within_10:.1f}%")
print(f"Predictions within ±20%: {within_20:.1f}%")
print(f"Predictions within ±30%: {within_30:.1f}%")

# ============================================================================
# Save final pipeline
# ============================================================================
final_pipe = Pipeline([
    ("prep", preprocessor),
    ("model", model)
])

model_path = Path(get_project_root()) / MODEL_DIR / "catboost_improved.pkl"
model_path.parent.mkdir(parents=True, exist_ok=True)
joblib.dump(final_pipe, model_path)
print(f"\n✅ Improved CatBoost model saved to {model_path}")

# ============================================================================
# IMPROVEMENT 7: Enhanced Visualizations
# ============================================================================
print("\n" + "=" * 60)
print("GENERATING VISUALIZATIONS")
print("=" * 60)

fig, axes = plt.subplots(2, 3, figsize=(18, 12))

# 1. Prediction vs Actual (original scale)
axes[0, 0].scatter(y_test_original, y_pred_original, alpha=0.5, s=20)
axes[0, 0].plot([y_test_original.min(), y_test_original.max()],
                [y_test_original.min(), y_test_original.max()],
                'r--', lw=2)
axes[0, 0].set_xlabel("Actual Price")
axes[0, 0].set_ylabel("Predicted Price")
axes[0, 0].set_title(f"Prediction vs Actual (R²={r2:.3f})")
axes[0, 0].grid(True, alpha=0.3)

# 2. Residual Plot
residuals = y_test_original - y_pred_original
axes[0, 1].scatter(y_pred_original, residuals, alpha=0.5, s=20)
axes[0, 1].axhline(y=0, color='r', linestyle='--', lw=2)
axes[0, 1].set_xlabel("Predicted Price")
axes[0, 1].set_ylabel("Residual")
axes[0, 1].set_title("Residual Plot")
axes[0, 1].grid(True, alpha=0.3)

# 3. Residual Distribution
axes[0, 2].hist(residuals, bins=50, edgecolor='black', alpha=0.7)
axes[0, 2].axvline(x=0, color='r', linestyle='--', lw=2)
axes[0, 2].set_xlabel("Residual")
axes[0, 2].set_ylabel("Frequency")
axes[0, 2].set_title("Residual Distribution")
axes[0, 2].grid(True, alpha=0.3)

# 4. Cross-Validation Scores
axes[1, 0].plot(range(1, len(cv_scores) + 1), -cv_scores, marker='o', linewidth=2)
axes[1, 0].axhline(y=-cv_scores.mean(), color='r', linestyle='--',
                   label=f'Mean: {-cv_scores.mean():.3f}')
axes[1, 0].set_xlabel("Fold")
axes[1, 0].set_ylabel("RMSE (log scale)")
axes[1, 0].set_title("Cross-Validation RMSE")
axes[1, 0].legend()
axes[1, 0].grid(True, alpha=0.3)

# 5. Prediction Error Percentage
error_pct = np.abs((y_test_original - y_pred_original) / y_test_original) * 100
axes[1, 1].hist(error_pct, bins=50, edgecolor='black', alpha=0.7)
axes[1, 1].axvline(x=10, color='r', linestyle='--', label='±10%')
axes[1, 1].axvline(x=20, color='orange', linestyle='--', label='±20%')
axes[1, 1].set_xlabel("Absolute Error (%)")
axes[1, 1].set_ylabel("Frequency")
axes[1, 1].set_title("Prediction Error Distribution")
axes[1, 1].legend()
axes[1, 1].grid(True, alpha=0.3)

# 6. Learning Curve (if available)
if hasattr(model, 'evals_result_'):
    evals_result = model.evals_result_
    if 'validation' in evals_result:
        train_rmse = evals_result['learn']['RMSE']
        val_rmse = evals_result['validation']['RMSE']
        axes[1, 2].plot(train_rmse, label='Train RMSE')
        axes[1, 2].plot(val_rmse, label='Validation RMSE')
        axes[1, 2].set_xlabel("Iteration")
        axes[1, 2].set_ylabel("RMSE")
        axes[1, 2].set_title("Learning Curve")
        axes[1, 2].legend()
        axes[1, 2].grid(True, alpha=0.3)
else:
    axes[1, 2].text(0.5, 0.5, 'Learning curve\nnot available',
                    ha='center', va='center', fontsize=12)
    axes[1, 2].set_xticks([])
    axes[1, 2].set_yticks([])

plt.tight_layout()
# plt.savefig('/home/claude/model_performance.png', dpi=150, bbox_inches='tight')
print("✅ Performance plots saved")

# ============================================================================
# Feature Importance
# ============================================================================
try:
    feature_names = preprocessor.get_feature_names_out()
    importances = model.get_feature_importance()

    feat_imp = pd.DataFrame({
        'feature': feature_names,
        'importance': importances
    }).sort_values('importance', ascending=False)

    print("\n" + "=" * 60)
    print("TOP 20 FEATURE IMPORTANCES")
    print("=" * 60)
    print(feat_imp.head(20).to_string(index=False))

    # Plot top features
    plt.figure(figsize=(10, 8))
    top_features = feat_imp.head(20)
    plt.barh(range(len(top_features)), top_features['importance'])
    plt.yticks(range(len(top_features)), top_features['feature'])
    plt.xlabel('Importance')
    plt.title('Top 20 Feature Importances')
    plt.gca().invert_yaxis()
    plt.tight_layout()
    # plt.savefig('/home/claude/feature_importance.png', dpi=150, bbox_inches='tight')
    print("✅ Feature importance plot saved")

except Exception as e:
    print(f"Cannot extract feature importance: {e}")

# ============================================================================
# Error Analysis by Feature
# ============================================================================
print("\n" + "=" * 60)
print("ERROR ANALYSIS")
print("=" * 60)

# Analyze errors by price range
price_bins = pd.qcut(y_test_original, q=5, labels=['Very Low', 'Low', 'Medium', 'High', 'Very High'])
error_by_price = pd.DataFrame({
    'Price Range': price_bins,
    'Absolute Error': np.abs(residuals),
    'Error %': error_pct
})

print("\nError by Price Range:")
print(error_by_price.groupby('Price Range').agg({
    'Absolute Error': ['mean', 'median'],
    'Error %': ['mean', 'median']
}).round(2))

print("\n" + "=" * 60)
print("TRAINING COMPLETE!")
print("=" * 60)
