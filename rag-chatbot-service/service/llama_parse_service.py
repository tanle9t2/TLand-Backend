import os
import tempfile

from dotenv import load_dotenv
from llama_parse import LlamaParse

load_dotenv()
parser = LlamaParse(api_key=os.getenv("LLAMA_PARSE_KEY"), result_type="markdown")


async def parse_markdown(uploaded_file):
    tmp_dir = tempfile.mkdtemp()
    tmp_path = os.path.join(tmp_dir, uploaded_file.filename)

    data = await uploaded_file.read()
    with open(tmp_path, "wb") as f:
        f.write(data)

    try:
        docs = await parser.aload_data(tmp_path)
        content = "\n".join([doc.text for doc in docs])

        return content

    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)  # delete file
        if os.path.exists(tmp_dir):
            os.rmdir(tmp_dir)  # delete empty temp folder
