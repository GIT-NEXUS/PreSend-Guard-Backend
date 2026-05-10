from fastapi import FastAPI
from pydantic import BaseModel
from transformers import pipeline
import re

app = FastAPI()

ner_pipeline = pipeline(
    "ner",
    model="Leo97/KoELECTRA-small-v3-modu-ner"
)

PII_LABELS = {"PS", "OG", "LC"}

LABEL_KO = {
    "PS": "사람이름",
    "OG": "기관/학교/회사/부서명",
    "LC": "지역명",
    "JB": "직업/직위",
}

# 긴 키워드가 먼저 매칭되도록 길이 내림차순 정렬
JOB_KEYWORDS = sorted([
    # 임원/직책
    "대표이사", "최고경영자", "부대표", "부회장", "부사장", "전무이사", "상무이사", "전무", "상무",
    "회장", "사장", "이사", "감사", "고문", "자문",
    "본부장", "부장", "차장", "과장", "대리", "주임", "사원",
    "팀장", "실장", "센터장", "원장", "단장", "국장", "처장", "소장",
    # 교육직
    "총장", "부총장", "학장", "교수", "부교수", "조교수", "강사", "조교",
    "교장", "교감", "교사", "선생님", "선생",
    # 전문직
    "외과의사", "내과의사", "한의사", "치과의사", "수의사", "의사",
    "간호사", "약사", "변호사", "검사", "판사", "변리사",
    "회계사", "세무사", "감정평가사", "공인중개사", "노무사", "건축사",
    # 공직/정치
    "국무총리", "대통령", "장관", "차관", "청장",
    "도지사", "구청장", "군수", "시장",
    "국회의원", "시의원", "도의원", "구의원", "의원",
    "소방청장", "소방서장", "소방관",
    "경찰청장", "경찰서장", "경찰관", "경찰",
    "군인", "장교", "공무원",
    # 언론/문화/IT
    "아나운서", "기자", "소설가", "시인", "화가", "배우", "가수", "감독", "작가",
    "엔지니어", "개발자", "디자이너", "기획자", "분석가", "연구원", "연구사",
    "사회복지사", "상담사",
], key=len, reverse=True)

class AnalyzeRequest(BaseModel):
    text: str

BIO_TAGS = {"B", "I", "E", "S"}

def parse_entities(results):
    entities = []
    current = None

    for item in results:
        raw_label = item["entity"]
        if "-" not in raw_label:
            if current:
                entities.append(current)
                current = None
            continue

        entity_type, bio = raw_label.rsplit("-", 1)
        # TYPE-B 형식이면 그대로, B-TYPE 형식이면 swap
        if bio not in BIO_TAGS:
            entity_type, bio = bio, entity_type
        if bio not in BIO_TAGS:
            if current:
                entities.append(current)
                current = None
            continue

        word = item["word"].replace("##", "")

        if bio in ("B", "S"):
            if current:
                entities.append(current)
            current = {
                "text": word,
                "label": entity_type,
                "score": round(float(item["score"]), 4),
                "start": item["start"],
                "end": item["end"]
            }
            if bio == "S":
                entities.append(current)
                current = None
        elif bio in ("I", "E") and current and current["label"] == entity_type:
            current["text"] += word
            current["end"] = item["end"]
            current["score"] = round((current["score"] + float(item["score"])) / 2, 4)
            if bio == "E":
                entities.append(current)
                current = None
        else:
            if current:
                entities.append(current)
            current = None

    if current:
        entities.append(current)

    # 인접한 동일 타입 엔티티 병합 (글자 단위 분리 보정)
    merged = []
    for entity in entities:
        if merged and merged[-1]["label"] == entity["label"] and merged[-1]["end"] == entity["start"]:
            merged[-1]["text"] += entity["text"]
            merged[-1]["end"] = entity["end"]
            merged[-1]["score"] = round((merged[-1]["score"] + entity["score"]) / 2, 4)
        else:
            merged.append(entity)

    return merged

def find_job_entities(text: str) -> list:
    covered = []  # (start, end) 범위 추적으로 중복 매칭 방지
    entities = []

    for keyword in JOB_KEYWORDS:
        for m in re.finditer(re.escape(keyword), text):
            start, end = m.start(), m.end()
            if any(s <= start < e or s < end <= e for s, e in covered):
                continue
            covered.append((start, end))
            entities.append({
                "text": keyword,
                "label": "JB",
                "label_ko": "직업/직위",
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
    results = ner_pipeline(req.text)
    entities = parse_entities(results)

    pii_entities = [
        {**e, "label_ko": LABEL_KO.get(e["label"], e["label"])}
        for e in entities
        if e["label"] in PII_LABELS
    ]

    job_entities = find_job_entities(req.text)

    all_entities = sorted(pii_entities + job_entities, key=lambda x: x["start"])

    return {
        "message": "NER 분석 완료",
        "input_text": req.text,
        "entities": all_entities
    }
