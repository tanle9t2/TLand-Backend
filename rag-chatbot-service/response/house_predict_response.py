from response.camel_model import CamelModel


class HousePredictResponse(CamelModel):
    price_per_m2: float
    total_price_ty: float
    description: object
    amenities: object
