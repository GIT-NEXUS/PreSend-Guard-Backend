import re
import urllib.request

from fastapi import FastAPI
from pydantic import BaseModel
from transformers import (
    AutoModelForTokenClassification,
    AutoTokenizer,
    pipeline,
)

app = FastAPI()

KPF_LABELS_URL = (
    "https://raw.githubusercontent.com/KPF-bigkinds/BIGKINDS-LAB/"
    "main/KPF-BERT-NER/label.py"
)


def load_kpf_labels():
    with urllib.request.urlopen(KPF_LABELS_URL, timeout=30) as resp:
        src = resp.read().decode("utf-8")
    namespace = {}
    exec(src, namespace)
    return namespace["labels"]


_labels = load_kpf_labels()
_id2label = {i: l for i, l in enumerate(_labels)}
_label2id = {l: i for i, l in _id2label.items()}

_model = AutoModelForTokenClassification.from_pretrained(
    "KPF/KPF-bert-ner",
    num_labels=len(_labels),
    id2label=_id2label,
    label2id=_label2id,
)
_tokenizer = AutoTokenizer.from_pretrained("KPF/KPF-bert-ner")

ner_pipeline = pipeline(
    "ner",
    model=_model,
    tokenizer=_tokenizer,
    aggregation_strategy="simple",
)

PII_LABEL_MAP = {
    # 사람이름
    "PS_NAME": "사람이름",
    # 학교
    "OGG_EDUCATION": "학교명",
    # 회사
    "OGG_ECONOMY": "회사명",
    # 기관
    "OGG_POLITICS": "기관명",
    "OGG_MEDICINE": "기관명",
    "OGG_LAW": "기관명",
    "OGG_RELIGION": "기관명",
    "OGG_MEDIA": "기관명",
    "OGG_ART": "기관명",
    "OGG_SPORTS": "기관명",
    "OGG_SCIENCE": "기관명",
    "OGG_LIBRARY": "기관명",
    "OGG_HOTEL": "기관명",
    "OGG_FOOD": "기관명",
    "OGG_MILITARY": "기관명",
    "OGG_OTHERS": "기관명",
    # 지역
    "LCP_COUNTRY": "지역명",
    "LCP_PROVINCE": "지역명",
    "LCP_COUNTY": "지역명",
    "LCP_CITY": "지역명",
    "LCP_CAPITALCITY": "지역명",
    # 직업/직위
    "CV_POSITION": "직위",
    "CV_OCCUPATION": "직업",
}

# 긴 접미사가 먼저 매칭되도록 정렬
DEPT_SUFFIXES = sorted([
    "사업본부", "개발본부", "사업부", "개발실", "기획실", "연구실",
    "디자인실", "디자인팀", "연구소", "영업소",
    "본부", "팀", "센터", "지점", "지사", "파트", "그룹",
], key=len, reverse=True)

DEPT_PATTERN = re.compile(
    r"([가-힣A-Za-z0-9]{1,15})(" + "|".join(map(re.escape, DEPT_SUFFIXES)) + r")"
)


class AnalyzeRequest(BaseModel):
    text: str


MASK_TOKEN = "***"


def mask_text(text: str, entities: list) -> str:
    # entities는 start 오름차순 정렬돼 있고 서로 겹치지 않음을 가정
    result = []
    cursor = 0
    for e in entities:
        start, end = e["start"], e["end"]
        if start < cursor:  # 안전망: 겹치면 건너뜀
            continue
        result.append(text[cursor:start])
        result.append(MASK_TOKEN)
        cursor = end
    result.append(text[cursor:])
    return "".join(result)


def find_dept_entities(text: str) -> list:
    covered = []
    entities = []
    for m in DEPT_PATTERN.finditer(text):
        start, end = m.start(), m.end()
        if any(s <= start < e or s < end <= e for s, e in covered):
            continue
        # 최소 길이 2 (한 글자 부서명은 노이즈 가능성 높음)
        if end - start < 2:
            continue
        covered.append((start, end))
        entities.append({
            "text": text[start:end],
            "label": "DEPT",
            "label_ko": "부서명",
            "score": 1.0,
            "start": start,
            "end": end,
        })
    return entities


@app.get("/")
def root():
    return {"message": "NER server is running"}


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    text = req.text

    results = ner_pipeline(text)

    entities = [
        {
            "text": item["word"],
            "label": item["entity_group"],
            "score": round(float(item["score"]), 4),
            "start": item["start"],
            "end": item["end"],
        }
        for item in results
    ]

    return {
        "message": "NER 분석 완료",
        "input_text": text,
        "entities": all_entities,
        "masked_text": mask_text(text, all_entities),
    }
