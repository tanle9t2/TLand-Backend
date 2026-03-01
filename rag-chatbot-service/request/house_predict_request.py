from typing import Optional

from response.camel_model import CamelModel


class HousePredictRequest(CamelModel):
    area: float
    floors: int
    bedrooms: int
    bathrooms: int
    property_type: str
    property_feature: Optional[str] = None
    legal_status: Optional[str] = None
    furniture_state: Optional[str] = None
    address: str
    year: int
