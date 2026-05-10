import os
import ast
import json
import asyncio
import logging
from datetime import datetime
from pathlib import Path
from typing import Optional

import pandas as pd
import openai
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from ragas import SingleTurnSample
from ragas.llms import LangchainLLMWrapper
from ragas.embeddings import OpenAIEmbeddings
from ragas.metrics import (
    Faithfulness,
    AnswerRelevancy,
    ContextRecall,
    ContextPrecision,
    AnswerCorrectness,
)

from service.rag_qa import ask_question

load_dotenv()

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

PROJECT_ROOT = Path(__file__).parent.parent
TEST_CASES_PATH = PROJECT_ROOT / "evaluator_reports" / "evaluator_test_cases.csv"
REPORTS_DIR = PROJECT_ROOT / "evaluator_reports"
REPORTS_DIR.mkdir(parents=True, exist_ok=True)

openai_client = openai.OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
evaluator_llm = LangchainLLMWrapper(ChatOpenAI(model="gpt-4o-mini", temperature=0))


class FixedOpenAIEmbeddings(OpenAIEmbeddings):
    def embed_query(self, text: str):
        return self.embed_text(text)

    def embed_documents(self, texts):
        if isinstance(texts, str):
            texts = [texts]
        return [self.embed_text(t) for t in texts]


evaluator_embeddings = FixedOpenAIEmbeddings(
    model="text-embedding-3-small",
    client=openai_client,
)

METRIC_NAMES = [
    "faithfulness",
    "answer_relevancy",
    "context_recall",
    "context_precision",
]

_EVAL_CONCURRENCY = 3


def _make_metrics() -> dict:
    llm = LangchainLLMWrapper(ChatOpenAI(model="gpt-4o-mini", temperature=0))
    embeddings = FixedOpenAIEmbeddings(
        model="text-embedding-3-small",
        client=openai_client,
    )
    return {
        "faithfulness": Faithfulness(llm=llm),
        "answer_relevancy": AnswerRelevancy(llm=llm, embeddings=embeddings),
        "context_recall": ContextRecall(llm=llm),
        "context_precision": ContextPrecision(llm=llm),
    }


NO_INFO_KEYWORDS = [
    "không có", "không tìm thấy", "chưa có thông tin",
    "không có dữ liệu", "không hỗ trợ", "chưa cập nhật",
    "chưa có dữ liệu", "không tìm được", "liên hệ luật sư",
    "liên hệ trực tiếp", "chưa tìm thấy",
]

INTENT_REQUIRED_FIELDS: dict[str, list[str]] = {
    "BANK_LOAN": ["message", "loanPackages", "financialAdvice", "followUpQuestion"],
    "LEGAL": ["message", "legalPoints", "riskWarnings", "followUpQuestion"],
    "CONSULTATION": ["message", "properties", "followUpQuestion"],
    "PROPERTY_DETAIL": ["message", "property", "agentAnalysis", "followUpQuestion"],
    "MARKET_ANALYSIS": ["message", "followUpQuestion"],
    "UNCLEAR": ["message", "properties"],
}


def extract_answer_text(answer) -> str:
    if not isinstance(answer, dict):
        return str(answer)

    parts: list[str] = []

    if answer.get("message"):
        parts.append(str(answer["message"]))

    for pkg in answer.get("loanPackages") or []:
        if isinstance(pkg, dict):
            bank = pkg.get("bankName", "")
            rate = pkg.get("interestRate", "")
            ratio = pkg.get("maxLoanRatio", "")
            term = pkg.get("maxTerm", "")
            monthly = pkg.get("monthlyEstimate", "")
            parts.append(
                f"{bank}: lãi suất {rate}, tỷ lệ {ratio}, kỳ hạn {term}, ước tính {monthly}"
            )
        else:
            parts.append(json.dumps(pkg, ensure_ascii=False))

    if answer.get("financialAdvice"):
        parts.append(str(answer["financialAdvice"]))

    for lp in answer.get("legalPoints") or []:
        if isinstance(lp, dict):
            parts.append(f"{lp.get('title', '')} {lp.get('content', '')}")
        else:
            parts.append(str(lp))

    for rw in answer.get("riskWarnings") or []:
        parts.append(str(rw))

    for prop in answer.get("properties") or []:
        if isinstance(prop, dict):
            parts.append(
                " ".join(filter(None, [
                    prop.get("title", ""),
                    prop.get("price", ""),
                    prop.get("location", ""),
                    prop.get("specs", ""),
                    " ".join(prop.get("highlights") or []),
                    prop.get("agentNote", ""),
                ]))
            )
        else:
            parts.append(str(prop))

    prop_detail = answer.get("property")
    if isinstance(prop_detail, dict):
        parts.append(
            " ".join(filter(None, [
                prop_detail.get("title", ""),
                prop_detail.get("price", ""),
                prop_detail.get("location", ""),
                prop_detail.get("specs", ""),
                prop_detail.get("description", ""),
                prop_detail.get("legalStatus", ""),
                prop_detail.get("furnitureState", ""),
                " ".join(prop_detail.get("highlights") or []),
            ]))
        )

    pe = answer.get("priceEvaluation")
    if isinstance(pe, dict):
        parts.append(
            " ".join(filter(None, [
                f"Giá dự báo: {pe.get('predictedPrice', '')}",
                f"Giá niêm yết: {pe.get('listedPrice', '')}",
                f"Đánh giá: {pe.get('verdict', '')}",
                f"Chênh lệch: {pe.get('diffPercent', '')}",
                pe.get("analysis", ""),
            ]))
        )

    if answer.get("agentAnalysis"):
        parts.append(str(answer["agentAnalysis"]))

    if answer.get("followUpQuestion"):
        parts.append(str(answer["followUpQuestion"]))

    return " ".join(parts).strip()


def extract_contexts(raw_contexts) -> list[str]:
    texts = []
    for item in raw_contexts:
        if isinstance(item, dict):
            texts.append(item.get("text", ""))
        else:
            texts.append(str(item))
    return [t for t in texts if t]


def detect_intent_from_answer(answer_raw) -> str:
    if not isinstance(answer_raw, dict):
        return "UNKNOWN"
    keys = set(answer_raw.keys())
    if "loanPackages" in keys:
        return "BANK_LOAN"
    if "legalPoints" in keys or "riskWarnings" in keys:
        return "LEGAL"
    if "property" in keys or "priceEvaluation" in keys or "agentAnalysis" in keys:
        return "PROPERTY_DETAIL"
    if "properties" in keys:
        # CONSULTATION and UNCLEAR both have `properties`
        props = answer_raw.get("properties")
        if isinstance(props, list) and len(props) == 0:
            return "CONSULTATION"  # either UNCLEAR or CONSULTATION—treat the same
        return "CONSULTATION"
    if "marketSummary" in keys or "priceRange" in keys:
        return "MARKET_ANALYSIS"
    return "UNKNOWN"


def validate_json_structure(answer_raw, intent: str) -> tuple[bool, list[str]]:
    if not isinstance(answer_raw, dict):
        return False, ["answer_is_not_dict"]
    required = INTENT_REQUIRED_FIELDS.get(intent, ["message"])
    missing = [f for f in required if f not in answer_raw]
    return len(missing) == 0, missing


def is_no_info_answer(answer_raw, answer_text: str) -> bool:
    text_lower = answer_text.lower()

    if any(kw in text_lower for kw in NO_INFO_KEYWORDS):
        return True

    if not isinstance(answer_raw, dict):
        return False

    msg_lower = str(answer_raw.get("message", "")).lower()

    # BANK_LOAN: empty loanPackages + message about no data
    loan_pkgs = answer_raw.get("loanPackages")
    if loan_pkgs is not None and loan_pkgs == []:
        if any(kw in msg_lower for kw in ["chưa có", "chưa cập nhật", "liên hệ", "không có"]):
            return True

    # LEGAL: empty legalPoints + message about no data
    legal_pts = answer_raw.get("legalPoints")
    if legal_pts is not None and legal_pts == []:
        if any(kw in msg_lower for kw in ["chưa có", "không có", "liên hệ", "tra cứu"]):
            return True

    return False


def parse_history(raw) -> list:
    if not raw or isinstance(raw, float):
        return []
    if isinstance(raw, list):
        return raw
    raw = str(raw).strip()
    if not raw or raw in ("[]", ""):
        return []
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        pass
    try:
        return ast.literal_eval(raw)
    except Exception:
        logger.warning("Failed to parse history: %s", raw[:100])
        return []

_MIN_RESPONSE_LEN = 15


async def evaluate_sample(row: dict) -> dict:
    question: str = row["question"]
    expected_answer: str = str(row.get("expected_answer", ""))
    history: list = parse_history(row.get("history_json"))
    no_info_case: bool = bool(int(row.get("no_info_case", 0)))

    logger.info("Evaluating: %s", question[:80])
    try:
        rag_result = await ask_question(question, history)
    except Exception as exc:
        logger.error("ask_question failed for '%s': %s", question[:60], exc)
        return {
            "question": question,
            "intent_hint": row.get("intent_hint", ""),
            "detected_intent": "ERROR",
            "reference": expected_answer,
            "rag_answer": "ERROR",
            "context_count": 0,
            "context": "",
            "json_structure_valid": False,
            "missing_fields": "ask_question_exception",
            "no_info_case": no_info_case,
            "no_info_detected": False,
            "error": str(exc),
        }

    rag_answer_raw = rag_result.get("answer", "")
    raw_contexts = rag_result.get("context", [])
    detected_intent: str = rag_result.get("intent")

    rag_answer_text: str = extract_answer_text(rag_answer_raw)
    rag_contexts: list[str] = extract_contexts(raw_contexts)

    json_valid, missing_fields_list = validate_json_structure(rag_answer_raw, detected_intent)

    result = {
        "question": question,
        "intent_hint": row.get("intent_hint", ""),
        "detected_intent": detected_intent,
        "reference": expected_answer,
        "rag_answer": rag_answer_text,
        "context_count": len(rag_contexts),
        "context": " | ".join(rag_contexts),
        "json_structure_valid": json_valid,
        "missing_fields": ", ".join(missing_fields_list) if missing_fields_list else "",
        "no_info_case": no_info_case,
    }

    # --- No-info shortcut scoring ---
    if no_info_case:
        detected = is_no_info_answer(rag_answer_raw, rag_answer_text)
        result["faithfulness_score"] = 1.0 if detected else 0.0
        result["answer_relevancy_score"] = 1.0 if detected else 0.0
        result["context_recall_score"] = 1.0
        result["context_precision_score"] = 1.0
        result["answer_correctness_score"] = 1.0 if detected else 0.0
        result["no_info_detected"] = detected
        return result

    result["no_info_detected"] = is_no_info_answer(rag_answer_raw, rag_answer_text)
    has_contexts = bool(rag_contexts)

    # Filter out empty/whitespace-only context strings to avoid metric failures
    clean_contexts = [c for c in rag_contexts if c and c.strip()]
    has_clean_contexts = bool(clean_contexts)

    # answer_relevancy requires a meaningful response to generate sentences from;
    # skip it if the response is too short to avoid 'list index out of range'.
    response_too_short = len(rag_answer_text.strip()) < _MIN_RESPONSE_LEN
    if response_too_short:
        logger.warning(
            "answer_relevancy skipped for '%s': response too short (%d chars)",
            question[:60], len(rag_answer_text.strip()),
        )

    # --- RAGAS scoring ---
    ragas_sample = SingleTurnSample(
        user_input=question,
        response=rag_answer_text if rag_answer_text.strip() else "(no response)",
        retrieved_contexts=clean_contexts if has_clean_contexts else ["(no context)"],
        reference=expected_answer,
    )

    metrics = _make_metrics()

    for name, metric in metrics.items():
        logger.info(f"[DEBUG] === Processing metric: {name} ===")

        # Debug input state
        logger.debug(f"[DEBUG] has_clean_contexts={has_clean_contexts}")
        logger.debug(f"[DEBUG] response_too_short={response_too_short}")

        if name in ("context_recall", "context_precision"):
            if not has_clean_contexts:
                logger.warning(
                    f"[DEBUG] SKIP {name} — no clean contexts available"
                )
                result[f"{name}_score"] = None
                continue
            else:
                logger.debug(f"[DEBUG] {name} — context available, continue scoring")

        if name == "answer_relevancy":
            if response_too_short:
                logger.warning(
                    f"[DEBUG] SKIP {name} — response too short"
                )
                result[f"{name}_score"] = None
                continue
            else:
                logger.debug(f"[DEBUG] {name} — response length OK")

        try:
            logger.info(f"[DEBUG] Running metric: {name}")

            score = await metric.single_turn_ascore(sample=ragas_sample)

            logger.info(
                f"[DEBUG] Metric '{name}' SUCCESS — score={score}"
            )

            result[f"{name}_score"] = score

        except IndexError as exc:
            logger.warning(
                f"[DEBUG] Metric '{name}' SKIPPED — IndexError (likely empty response/context): {exc}"
            )
            result[f"{name}_score"] = None

        except Exception as exc:
            logger.error(
                f"[DEBUG] Metric '{name}' FAILED — Exception: {exc}",
                exc_info=True
            )
            result[f"{name}_score"] = None


    for name in METRIC_NAMES:
        if f"{name}_score" not in result:
            result[f"{name}_score"] = None

    return result


async def evaluate_batch(samples: list[dict]) -> list[dict]:
    sem = asyncio.Semaphore(_EVAL_CONCURRENCY)
    total = len(samples)

    async def _bounded(i: int, row: dict) -> dict:
        async with sem:
            logger.info("[%d/%d] Evaluating: %s", i, total, str(row.get("question", ""))[:80])
            return await evaluate_sample(row)

    tasks = [_bounded(i, row) for i, row in enumerate(samples, 1)]
    raw = await asyncio.gather(*tasks, return_exceptions=True)

    results = []
    for i, item in enumerate(raw, 1):
        if isinstance(item, BaseException):
            logger.error("Sample %d failed unexpectedly: %s", i, item)
        else:
            results.append(item)
    return results


def load_test_cases(csv_path: Path = TEST_CASES_PATH) -> list[dict]:
    """Load test cases from CSV file."""
    df = pd.read_csv(csv_path, encoding="utf-8-sig")
    required = {"question", "expected_answer"}
    missing_cols = required - set(df.columns)
    if missing_cols:
        raise ValueError(f"CSV is missing required columns: {missing_cols}")
    return df.to_dict(orient="records")


SCORE_COLS = [f"{m}_score" for m in METRIC_NAMES]


def build_per_testcase_table(df: pd.DataFrame) -> pd.DataFrame:
    """Build a per-testcase score table sorted by overall_score ascending."""
    score_cols = [c for c in SCORE_COLS if c in df.columns]
    numeric = df[score_cols].apply(pd.to_numeric, errors="coerce")

    result = pd.DataFrame()
    result["question"] = df["question"].values
    result["intent_hint"] = df.get("intent_hint", "").values
    result["detected_intent"] = df.get("detected_intent", "").values
    result["json_valid"] = df.get("json_structure_valid", True).values
    result["no_info_case"] = df.get("no_info_case", False).values

    for col in score_cols:
        result[col] = numeric[col].values

    # Overall score = mean of all non-null metric scores per row
    result["overall_score"] = numeric.mean(axis=1)
    result["pass"] = result["overall_score"] >= 0.6

    return result.sort_values("overall_score", ascending=True).reset_index(drop=True)


def build_summary(df: pd.DataFrame) -> pd.DataFrame:
    numeric = df[SCORE_COLS].apply(pd.to_numeric, errors="coerce")

    # Overall summary
    overall = numeric.mean().rename("mean_score").to_frame()
    overall["evaluated_count"] = numeric.count()
    overall["null_count"] = numeric.isnull().sum()
    overall.index.name = "metric"

    if "json_structure_valid" in df.columns:
        valid_rate = df["json_structure_valid"].mean() * 100
        logger.info("JSON structure validity: %.1f%%", valid_rate)

    # Per-intent breakdown
    if "detected_intent" in df.columns:
        intent_groups = []
        for intent, grp in df.groupby("detected_intent"):
            grp_numeric = grp[SCORE_COLS].apply(pd.to_numeric, errors="coerce")
            row = grp_numeric.mean()
            row["count"] = len(grp)
            row.name = intent
            intent_groups.append(row)
        if intent_groups:
            intent_df = pd.DataFrame(intent_groups)
            logger.info("Per-intent score breakdown:\n%s", intent_df.to_string())

    return overall


def save_report(results: list[dict], timestamp: str | None = None) -> Path:
    if timestamp is None:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    df = pd.DataFrame(results)
    leading = [
        "question", "intent_hint", "detected_intent",
        "no_info_case", "no_info_detected",
        "json_structure_valid", "missing_fields",
        "reference", "rag_answer", "context_count",
    ]
    score_cols = [c for c in SCORE_COLS if c in df.columns]
    trailing = ["context", "error"]
    ordered = leading + score_cols + trailing
    df = df[[c for c in ordered if c in df.columns]]

    detail_path = REPORTS_DIR / f"ragas_metrics_report_{timestamp}.csv"
    df.to_csv(detail_path, index=False, encoding="utf-8-sig")
    logger.info("Detailed report saved → %s", detail_path)

    return detail_path


if __name__ == "__main__":
    logger.info("Loading test cases from %s", TEST_CASES_PATH)
    samples = load_test_cases()
    logger.info("Loaded %d test cases", len(samples))

    all_results = asyncio.run(evaluate_batch(samples))
    save_report(all_results)
