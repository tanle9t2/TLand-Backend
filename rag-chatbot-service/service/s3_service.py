import boto3
import uuid
import io
import os
from dotenv import load_dotenv

load_dotenv()

AWS_ACCESS_KEY_ID = os.getenv("AWS_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY = os.getenv("AWS_SECRET_ACCESS_KEY")
AWS_REGION = os.getenv("AWS_REGION")
AWS_BUCKET_NAME = os.getenv("AWS_BUCKET_NAME")

s3_client = boto3.client(
    "s3",
    region_name=AWS_REGION,
    aws_access_key_id=AWS_ACCESS_KEY_ID,
    aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
)


def upload_file_to_s3(file):
    return upload_content_to_s3(file.file, file.filename)


def upload_content_to_s3(file_obj, filename):
    file_extension = filename.split(".")[-1]
    unique_filename = f"{uuid.uuid4()}.{file_extension}"

    if isinstance(file_obj, bytes):
        file_obj = io.BytesIO(file_obj)

    s3_client.upload_fileobj(
        file_obj,
        AWS_BUCKET_NAME,
        unique_filename,
    )

    file_url = f"https://{AWS_BUCKET_NAME}.s3.{AWS_REGION}.amazonaws.com/{unique_filename}"

    return file_url
