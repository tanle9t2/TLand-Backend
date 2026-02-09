from pathlib import Path

def get_project_root():
    return Path(__file__).resolve().parents[1]
    # utils -> rag-chatbot-service

def get_data_path(filename):
    return get_project_root() / "data" / filename
