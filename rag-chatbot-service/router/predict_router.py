from fastapi import APIRouter

from request.house_predict_request import HousePredictRequest
from response.house_predict_response import HousePredictResponse
from service.house_predict_service import HouseService

router = APIRouter(
    prefix="/api/v1/predict",
    tags=["House Prediction"]
)

from fastapi import status


@router.post(
    "/",
    response_model=HousePredictResponse,
    status_code=status.HTTP_200_OK
)
def predict_house(request: HousePredictRequest):
    return HouseService.predict(request.model_dump())
