from fastapi import FastAPI
from pydantic import BaseModel
from transformers import pipeline

app = FastAPI()

ner_pipeline = pipeline(
    "ner",
    model="Leo97/KoELECTRA-small-v3-modu-ner"
)

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
        "input_text": req.text,
        "entities": entities
    }
