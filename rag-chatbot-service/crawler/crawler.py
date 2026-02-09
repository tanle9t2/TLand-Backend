import random
import re
from datetime import datetime

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
import pandas as pd
import time

from utils.helper import get_project_root, get_data_path

URL = "https://www.nhatot.com/mua-ban-nha-dat-tp-ho-chi-minh"

column_order = [
    "address",
    "area",
    "frontage",
    "access_road",
    "house_direction",
    "balcony_direction",
    "floors",
    "bedrooms",
    "bathrooms",
    "legal_status",
    "furniture_state",
    "price",
    "year"
]


def init_driver():
    options = webdriver.ChromeOptions()
    options.add_argument("--start-maximized")
    options.add_argument("--disable-blink-features=AutomationControlled")

    driver = webdriver.Chrome(
        service=Service(ChromeDriverManager().install()),
        options=options
    )
    return driver


# ----------------------
# Scroll để load thêm tin
# ----------------------
def scroll_page(driver, times=8):
    for i in range(times):
        driver.execute_script(
            "window.scrollTo(0, document.body.scrollHeight);"
        )
        time.sleep(2)


def human_delay(min_s=3, max_s=7):
    t = random.uniform(min_s, max_s)
    print(f"Sleeping {t:.1f}s...")
    time.sleep(t)


def get_listing_links(driver):
    links = set()

    elements = driver.find_elements(By.CSS_SELECTOR, "a[href*='/mua-ban']")

    for el in elements:
        link = el.get_attribute("href")
        if link and "nhatot.com" in link and link.endswith(".htm"):
            links.add(link)

    return list(links)


def extract_number(text):
    if not text:
        return None
    text = text.replace(",", ".")
    match = re.search(r"\d+(\.\d+)?", text)
    return float(match.group()) if match else None


def extract_int(text):
    if not text:
        return None
    match = re.search(r"\d+", text)
    return int(match.group()) if match else 1


def normalize_furniture(value):
    if not value:
        return None

    value = value.lower()

    if "đầy đủ" in value:
        return "Full"
    elif "cơ bản" in value:
        return "Basic"
    elif "không" in value:
        return "None"
    else:
        return "Other"


def normalize_legal_status(value):
    if not value:
        return None

    value = value.lower()

    if "đã có sổ" in value:
        return "Have certificate"
    elif "sổ chung" in value:
        return "Shared certificate"
    elif "đang chờ sổ" in value or "chưa có sổ" in value:
        return "No certificate"
    else:
        return "Other"


def get_detail(driver, link):
    print("Crawling:", link)

    driver.get(link)
    time.sleep(5)

    price = None
    address_detail = None
    properties_data = {}

    # ADDRESS
    try:
        address_block = driver.find_element(
            By.XPATH,
            "/html/body/div[1]/div/div[4]/div[1]/div/div[2]/div[2]/div/div[2]/div/div/div/div[4]/div[1]/div"
        )
        spans = address_block.find_elements(By.TAG_NAME, "span")

        if len(spans) >= 2:
            address_detail = spans[0].text.split(",")[0] + ", " + spans[1].text[1:-5]


    except:
        pass

    # PRICE
    try:
        price = driver.find_element(By.XPATH
                                    ,
                                    "/html/body/div[1]/div/div[4]/div[1]/div/div[2]/div[2]/div/div[2]/div/div/div/div[3]/div[1]/div/b").text

    except:
        pass

    # PROPERTIES
    try:
        block = driver.find_element(By.XPATH,
                                    "/html/body/div[1]/div/div[4]/div[1]/div/div[2]/div[2]/div/div[3]/div/div")
        items = block.find_elements(By.TAG_NAME, "div")
        raw_properties = {}

        for item in items:
            spans = item.find_elements(By.TAG_NAME, "span")
            strongs = item.find_elements(By.TAG_NAME, "strong")

            if spans and strongs:
                key = spans[0].text.strip()
                value = strongs[0].text.strip()
                raw_properties[key] = value

        properties_data = {
            "area": extract_number(
                raw_properties.get("Diện tích đất")
                or raw_properties.get("Diện tích")
            ),
            "floors": extract_int(raw_properties.get("Tổng số tầng")),
            "bedrooms": extract_int(raw_properties.get("Số phòng ngủ")),
            "bathrooms": extract_int(raw_properties.get("Số phòng vệ sinh")),
            "house_direction": raw_properties.get("Hướng cửa chính") if raw_properties.get("Hướng cửa chính") else None,
            "balcony_direction": raw_properties.get("Hướng ban công") if raw_properties.get("Hướng ban công") else None,
            "legal_status": normalize_legal_status(
                raw_properties.get("Giấy tờ pháp lý")
            ) if raw_properties.get("Giấy tờ pháp lý") else None,
            "furniture_state": normalize_furniture(
                raw_properties.get("Tình trạng nội thất")
            ) if raw_properties.get("Tình trạng nội thất") else None
        }


    except:
        pass

    result = {
        "price": extract_number(price),
        "address": address_detail,
        "year": datetime.now().year
    }

    result.update(properties_data)
    return result


def crawl(start_page=1, end_page=5):
    driver = init_driver()
    seen_links = set()

    file_path = get_data_path("../data/nhatot.csv")

    for page in range(start_page, end_page + 1):

        url = f"{URL}?page={page}"
        print(f"\n===== PAGE {page} =====")

        driver.get(url)
        time.sleep(3)

        scroll_page(driver, 2)

        links = get_listing_links(driver)
        print("Found links:", len(links))

        page_data = []  # lưu dữ liệu của page này

        for link in links:

            if link in seen_links:
                continue
            seen_links.add(link)

            try:
                row = get_detail(driver, link)
                print("RESULT:", row)

                page_data.append(row)

            except Exception as e:
                print("ERROR at link:", link, e)

            human_delay()

        if page_data:
            df_page = pd.DataFrame(page_data)
            df_page = df_page.reindex(columns=column_order)
            df_page.to_csv(
                file_path,
                mode="a",
                header=not file_path.exists(),
                index=False,
                encoding="utf-8-sig"
            )
            print(f"Saved page {page}")

    driver.quit()


if __name__ == "__main__":
    crawl(start_page=350, end_page=500)
    print("Crawl finished")
