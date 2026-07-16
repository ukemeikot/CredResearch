"""AI endpoints in stub mode (no LLM configured) return schema-valid JSON (FR-AI, graceful degrade)."""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_topics_returns_ideas():
    r = client.post("/ai/topics", json={"field": "machine learning", "level": "MSc"})
    assert r.status_code == 200
    body = r.json()
    assert len(body["topics"]) >= 1
    assert body["topics"][0]["feasibility"] in {"LOW", "MEDIUM", "HIGH"}


def test_objectives_shape():
    r = client.post("/ai/objectives", json={"topic": "AI in radiology", "level": "MSc"})
    assert r.status_code == 200
    body = r.json()
    assert body["aim"]
    assert len(body["objectives"]) >= 3
    assert len(body["research_questions"]) >= 1


def test_alignment_flags_empty_sections():
    r = client.post(
        "/ai/alignment",
        json={"title": "T", "objectives": ["a"], "sections": [{"heading": "Intro", "text": ""}]},
    )
    assert r.status_code == 200
    body = r.json()
    assert 0 <= body["overall_score"] <= 100
    assert isinstance(body["findings"], list)


def test_section_assist_returns_suggestion():
    r = client.post("/ai/section-assist", json={"heading": "Background", "current_text": ""})
    assert r.status_code == 200
    assert r.json()["suggestion"]
