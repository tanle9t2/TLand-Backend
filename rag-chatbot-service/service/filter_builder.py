"""
filter_builder.py
Auto-extract Pinecone metadata filters from Vietnamese real estate queries.
Detects: price range, district/ward, property type (buy/rent), area, bedrooms, bathrooms, floors.
"""

import re
from typing import Optional

# Common Vietnamese districts in HCMC
_DISTRICT_MAP = {
    "gò vấp": "Gò Vấp", "go vap": "Gò Vấp",
    "bình thạnh": "Bình Thạnh", "binh thanh": "Bình Thạnh",
    "thủ đức": "Thủ Đức", "thu duc": "Thủ Đức",
    "quận 1": "Quận 1", "q1": "Quận 1", "q.1": "Quận 1",
    "quận 2": "Quận 2", "q2": "Quận 2", "q.2": "Quận 2",
    "quận 3": "Quận 3", "q3": "Quận 3",
    "quận 4": "Quận 4", "q4": "Quận 4",
    "quận 5": "Quận 5", "q5": "Quận 5",
    "quận 6": "Quận 6", "q6": "Quận 6",
    "quận 7": "Quận 7", "q7": "Quận 7",
    "quận 8": "Quận 8", "q8": "Quận 8",
    "quận 9": "Quận 9", "q9": "Quận 9",
    "quận 10": "Quận 10", "q10": "Quận 10",
    "quận 11": "Quận 11", "q11": "Quận 11",
    "quận 12": "Quận 12", "q12": "Quận 12",
    "bình chánh": "Bình Chánh", "binh chanh": "Bình Chánh",
    "nhà bè": "Nhà Bè", "nha be": "Nhà Bè",
    "hóc môn": "Hóc Môn", "hoc mon": "Hóc Môn",
    "củ chi": "Củ Chi", "cu chi": "Củ Chi",
    "cần giờ": "Cần Giờ", "can gio": "Cần Giờ",
    "tân bình": "Tân Bình", "tan binh": "Tân Bình",
    "tân phú": "Tân Phú", "tan phu": "Tân Phú",
    "phú nhuận": "Phú Nhuận", "phu nhuan": "Phú Nhuận",
    "bình tân": "Bình Tân", "binh tan": "Bình Tân",
}

BUY_KEYWORDS = ["mua", "bán", "ban", "mua bán", "sang nhượng"]
RENT_KEYWORDS = ["thuê", "thue", "cho thuê", "phòng trọ", "phong tro", "trọ"]


def extract_filters(pre_condition: object, query: str) -> dict:
    """
    Parse a Vietnamese real estate query and return a Pinecone filter dict.

    Returns:
        dict: Pinecone-compatible $and filter, e.g.
              {"$and": [{"price": {"$lte": 3e9}}, {"post_type": {"$eq": "BAN"}}]}
              Returns {} if no filters detected.
    """
    query_lower = query.lower()
    conditions = [pre_condition]

    price_filter = _extract_price_filter(query_lower)
    if price_filter:
        conditions.append(price_filter)

    district = _extract_district(query_lower)
    if district:
        conditions.append({"ward": {"$eq": district}})

    post_type = _extract_type(query_lower)
    if post_type:
        conditions.append({"post_type": {"$eq": post_type}})

    area_filter = _extract_area_filter(query_lower)
    if area_filter:
        conditions.append(area_filter)

    bedrooms_filter = _extract_bedrooms_filter(query_lower)
    if bedrooms_filter:
        conditions.append(bedrooms_filter)

    bathrooms_filter = _extract_bathrooms_filter(query_lower)
    if bathrooms_filter:
        conditions.append(bathrooms_filter)

    floors_filter = _extract_floors_filter(query_lower)
    if floors_filter:
        conditions.append(floors_filter)

    if not conditions:
        return {}

    return {"$and": conditions} if len(conditions) > 1 else conditions[0]


def _extract_price_filter(query_lower: str) -> Optional[dict]:
    # Pattern: "dưới X tỷ", "dưới X triệu"
    under_ty = re.search(r"d[uư][oờ]i\s+(\d+(?:[.,]\d+)?)\s*t[yỷ]", query_lower)
    under_tr = re.search(r"d[uư][oờ]i\s+(\d+(?:[.,]\d+)?)\s*tri[eệ]u", query_lower)

    # Pattern: "X tỷ", "X - Y tỷ", "từ X đến Y tỷ"
    range_ty = re.search(
        r"t[ừu]\s*(\d+(?:[.,]\d+)?)\s*(?:đến|den|tới|toi|-)\s*(\d+(?:[.,]\d+)?)\s*t[yỷ]",
        query_lower,
    )
    exact_ty = re.search(r"(\d+(?:[.,]\d+)?)\s*t[yỷ]", query_lower)
    exact_tr = re.search(r"(\d+(?:[.,]\d+)?)\s*tri[eệ]u", query_lower)

    if under_ty:
        val = float(under_ty.group(1).replace(",", ".")) * 1_000_000_000
        return {"price": {"$lte": val}}

    if under_tr:
        val = float(under_tr.group(1).replace(",", ".")) * 1_000_000
        return {"price": {"$lte": val}}

    if range_ty:
        lo = float(range_ty.group(1).replace(",", ".")) * 1_000_000_000
        hi = float(range_ty.group(2).replace(",", ".")) * 1_000_000_000
        return {"price": {"$gte": lo, "$lte": hi}}

    if exact_ty:
        val = float(exact_ty.group(1).replace(",", ".")) * 1_000_000_000
        # Use ±20% tolerance
        return {"price": {"$gte": val * 0.8, "$lte": val * 1.2}}

    if exact_tr:
        val = float(exact_tr.group(1).replace(",", ".")) * 1_000_000
        return {"price": {"$gte": val * 0.8, "$lte": val * 1.2}}

    return None


def _extract_district(query_lower: str) -> Optional[str]:
    """Match district/ward name from query."""
    for keyword, canonical in _DISTRICT_MAP.items():
        if keyword in query_lower:
            return canonical
    return None


def _extract_type(query_lower: str) -> Optional[str]:
    """Detect BAN or THUE from query keywords."""
    for kw in RENT_KEYWORDS:
        if kw in query_lower:
            return "THUE"
    for kw in BUY_KEYWORDS:
        if kw in query_lower:
            return "BAN"
    return None


def _extract_area_filter(query_lower: str) -> Optional[dict]:
    """Extract area filter: '60m2', '60 m2', 'diện tích 60m2', 'dt 60'. ±20% tolerance."""
    patterns = [
        r"(?:di[eệ]n\s*t[ií]ch|dt)\s*(\d+(?:[.,]\d+)?)\s*m",
        r"(\d+(?:[.,]\d+)?)\s*m2",
        r"(\d+(?:[.,]\d+)?)\s*m²",
    ]
    for pattern in patterns:
        match = re.search(pattern, query_lower)
        if match:
            val = float(match.group(1).replace(",", "."))
            if val > 0:
                return {"land_area": {"$gte": val * 0.8, "$lte": val * 1.2}}
    return None


def _extract_bedrooms_filter(query_lower: str) -> Optional[dict]:
    """Extract bedrooms: '3 phòng ngủ', '3pn', '3 PN', '3 phong ngu'."""
    patterns = [
        r"(\d+)\s*(?:ph[oò]ng\s*ng[uủ]|phong\s*ngu)",
        r"(\d+)\s*pn\b",
    ]
    for pattern in patterns:
        match = re.search(pattern, query_lower)
        if match:
            val = int(match.group(1))
            if val > 0:
                return {"bedrooms": {"$eq": val}}
    return None


def _extract_bathrooms_filter(query_lower: str) -> Optional[dict]:
    """Extract bathrooms: '3 wc', '3 nhà wc', '3 phòng tắm', '3 toilet', '3 nhà vệ sinh'."""
    patterns = [
        r"(\d+)\s*(?:nh[aà]\s*)?wc\b",
        r"(\d+)\s*(?:ph[oò]ng\s*t[aắ]m|phong\s*tam)",
        r"(\d+)\s*(?:ph[oò]ng\s*v[eệ]\s*sinh|phong\s*ve\s*sinh)",
        r"(\d+)\s*(?:nh[aà]\s*v[eệ]\s*sinh|nha\s*ve\s*sinh)",
        r"(\d+)\s*toilet",
    ]
    for pattern in patterns:
        match = re.search(pattern, query_lower)
        if match:
            val = int(match.group(1))
            if val > 0:
                return {"bathrooms": {"$eq": val}}
    return None


def _extract_floors_filter(query_lower: str) -> Optional[dict]:
    """Extract floors: '2 tầng', '2 tang'."""
    match = re.search(r"(\d+)\s*t[aầ]ng", query_lower)
    if match:
        val = int(match.group(1))
        if val > 0:
            return {"floors": {"$eq": val}}
    return None
