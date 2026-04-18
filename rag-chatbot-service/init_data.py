import os

import pandas as pd
import requests
import time

from dotenv import load_dotenv

load_dotenv()
API_KEY = os.getenv("GOONG_API_KEY")


def geocode_goong(address):
    url = "https://rsapi.goong.io/geocode"
    params = {
        "address": address,
        "api_key": API_KEY
    }

    try:
        response = requests.get(url, params=params, timeout=10)
        data = response.json()

        if data.get("results"):
            location = data["results"][0]["geometry"]["location"]
            return location["lat"], location["lng"]
        else:
            return None, None

    except Exception as e:
        print("Error:", e)
        return None, None


df = pd.read_csv("D:\\code\\DA\\TLand-Backend\\rag-chatbot-service\\features\\geocode_cache2.csv")

for i, row in df.iloc[991:].iterrows():
    address = row["address"]

    lat, lng = geocode_goong(address)

    df.loc[i, "lat"] = lat
    df.loc[i, "lng"] = lng

    print(f"Done {i}: {address}", lat, lng)

    time.sleep(0.3)

df.to_csv("output.csv", index=False, encoding="utf-8-sig")
