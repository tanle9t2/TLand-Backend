from sklearn.base import BaseEstimator, TransformerMixin

class FeatureNameCleaner(BaseEstimator, TransformerMixin):
    def fit(self, X, y=None):
        return self

    def transform(self, X):
        X = X.copy()
        X.columns = X.columns.str.replace(r"[^\w]", "_", regex=True)
        return X