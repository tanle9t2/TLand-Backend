from fastapi import APIRouter
from fastapi import status
from request.house_predict_request import HousePredictRequest
from response.house_predict_response import HousePredictResponse
from service.house_predict_service import HouseService

router = APIRouter(
    prefix="/api/v1/pred",
    tags=["House Prediction"]
)


@router.post(
    "/pred-hcm",
    response_model=HousePredictResponse,
    status_code=status.HTTP_200_OK
)
async def predict_house(request: HousePredictRequest):
    print("Predict request received")
    result = await HouseService.predict(request.model_dump(), is_description=True)
    return result
