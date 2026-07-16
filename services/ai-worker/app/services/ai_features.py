"""Phase 4 AI features. Each builds a prompt, asks the LLM for strict JSON, and validates it into
a typed schema. If no LLM is configured (or it errors), a deterministic stub keeps the feature
working (graceful degradation, NFR-AVAIL-5) and lets the whole stack run before the model VM is up."""

from __future__ import annotations

import logging

from pydantic import BaseModel, ValidationError

from app.core.config import get_settings
from app.domain import schemas as s
from app.services.llm_gateway import LlmError, LlmGateway

log = logging.getLogger(__name__)

_SYS = (
    "You are a rigorous academic research assistant for African university students. "
    "Reply with ONLY a single JSON object matching the requested shape — no prose, no markdown."
)


def _gateway() -> LlmGateway:
    return LlmGateway(get_settings())


def _try_llm(user: str, model: type[BaseModel]):
    """Call the LLM and validate into `model`; return None on any failure so callers can stub."""
    try:
        raw = _gateway().generate_json(_SYS, user)
        return model.model_validate(raw)
    except (LlmError, ValidationError) as e:
        log.info("LLM unavailable/invalid, using stub: %s", type(e).__name__)
        return None


# ── Topic generator ──────────────────────────────────────────────────────────
def topics(req: s.TopicRequest) -> s.TopicResponse:
    user = (
        f"Generate 5 {req.level} research topics in the field of '{req.field}'. "
        f"Student interests: '{req.interests}'. For each: title, one-sentence rationale, "
        f"feasibility (LOW|MEDIUM|HIGH), and 2-3 suggested_methods. "
        'JSON shape: {"topics":[{"title","rationale","feasibility","suggested_methods":[]}]}'
    )
    return _try_llm(user, s.TopicResponse) or _stub_topics(req)


def _stub_topics(req: s.TopicRequest) -> s.TopicResponse:
    base = req.field.strip() or "your field"
    ideas = [
        s.TopicIdea(
            title=f"{base}: {angle}",
            rationale=f"Addresses a practical gap in {base} relevant to {req.interests or 'local contexts'}.",
            feasibility=feas,
            suggested_methods=methods,
        )
        for angle, feas, methods in [
            ("a systematic review of recent approaches", "HIGH", ["literature review", "thematic synthesis"]),
            ("an empirical case study", "MEDIUM", ["survey", "interviews"]),
            ("a comparative evaluation of methods", "MEDIUM", ["experiment", "benchmarking"]),
            ("a design-and-build prototype study", "MEDIUM", ["prototyping", "usability testing"]),
            ("a data-driven analysis", "HIGH", ["dataset analysis", "descriptive statistics"]),
        ]
    ]
    return s.TopicResponse(topics=ideas)


# ── Objectives / RQs / hypotheses ────────────────────────────────────────────
def objectives(req: s.ObjectivesRequest) -> s.ObjectivesResponse:
    user = (
        f"For the {req.level} topic '{req.topic}' (problem: '{req.problem}'), produce one overarching "
        f"aim, 3-5 specific measurable objectives, matching research_questions (one per objective), "
        f"and 0-3 hypotheses if applicable. "
        'JSON: {"aim","objectives":[],"research_questions":[],"hypotheses":[]}'
    )
    return _try_llm(user, s.ObjectivesResponse) or _stub_objectives(req)


def _stub_objectives(req: s.ObjectivesRequest) -> s.ObjectivesResponse:
    t = req.topic.strip() or "the study"
    objs = [
        f"To review existing literature relevant to {t}.",
        f"To identify the key factors affecting {t}.",
        f"To evaluate approaches addressing {t}.",
        f"To recommend improvements based on the findings for {t}.",
    ]
    return s.ObjectivesResponse(
        aim=f"To investigate {t} and propose evidence-based recommendations.",
        objectives=objs,
        research_questions=[o.replace("To ", "What is required to ").rstrip(".") + "?" for o in objs],
        hypotheses=[f"There is a significant relationship between the identified factors and {t}."],
    )


# ── Problem statement ─────────────────────────────────────────────────────────
def problem_statement(req: s.ProblemStatementRequest) -> s.ProblemStatementResponse:
    user = (
        f"Write a concise academic problem statement and a significance paragraph for the topic "
        f"'{req.topic}'. Context: '{req.context}'. "
        'JSON: {"problem_statement","significance"}'
    )
    return _try_llm(user, s.ProblemStatementResponse) or s.ProblemStatementResponse(
        problem_statement=(
            f"Despite growing interest in {req.topic}, there remains a gap in understanding and "
            f"addressing its core challenges, which limits practical progress."
        ),
        significance=(
            f"This study contributes to {req.topic} by clarifying the problem and offering "
            f"actionable insights for researchers and practitioners."
        ),
    )


# ── Section assistant ────────────────────────────────────────────────────────
def section_assist(req: s.SectionAssistRequest) -> s.SectionAssistResponse:
    user = (
        f"Section heading: '{req.heading}'. Guidance: '{req.guidance}'. "
        f"Instruction: {req.instruction}. Current draft: '''{req.current_text[:4000]}'''. "
        'Return improved prose for this section. JSON: {"suggestion","notes":[]}'
    )
    return _try_llm(user, s.SectionAssistResponse) or s.SectionAssistResponse(
        suggestion=(
            (req.current_text.strip() + "\n\n") if req.current_text.strip() else ""
        )
        + f"[Draft for '{req.heading}'] Expand this section with a clear opening that states its "
        f"purpose, supporting points with evidence, and a sentence linking back to the study's aim.",
        notes=["Connect this section to your objectives.", "Cite sources where you make claims."],
    )


# ── Research alignment engine ────────────────────────────────────────────────
def alignment(req: s.AlignmentRequest) -> s.AlignmentResponse:
    joined = "; ".join(f"{x.heading}: {x.text[:300]}" for x in req.sections)
    user = (
        f"Assess how well this research aligns end-to-end. Title: '{req.title}'. Abstract: "
        f"'{req.abstract}'. Objectives: {req.objectives}. Sections: {joined}. "
        f"Score overall_score 0-100, give a summary, and list findings "
        f"[{{area,issue,suggestion,severity(LOW|MEDIUM|HIGH)}}]. "
        'JSON: {"overall_score","summary","findings":[]}'
    )
    return _try_llm(user, s.AlignmentResponse) or _stub_alignment(req)


def _stub_alignment(req: s.AlignmentRequest) -> s.AlignmentResponse:
    findings: list[s.AlignmentFinding] = []
    if not req.abstract:
        findings.append(s.AlignmentFinding(area="Abstract", issue="No abstract provided.",
                                           suggestion="Add a 150-300 word abstract.", severity="MEDIUM"))
    if len(req.objectives) < 3:
        findings.append(s.AlignmentFinding(area="Objectives", issue="Fewer than 3 objectives.",
                                           suggestion="State 3-5 specific, measurable objectives.", severity="HIGH"))
    empty = [x.heading for x in req.sections if not x.text.strip()]
    if empty:
        findings.append(s.AlignmentFinding(area="Coverage", issue=f"Empty sections: {', '.join(empty[:5])}.",
                                           suggestion="Draft the empty sections.", severity="MEDIUM"))
    score = max(20, 100 - len(findings) * 20 - len(empty) * 5)
    return s.AlignmentResponse(
        overall_score=min(score, 100),
        summary="Heuristic alignment check (LLM offline): "
        + ("looks reasonably complete." if not findings else "several gaps to address."),
        findings=findings or [s.AlignmentFinding(area="Overall", issue="No major gaps detected.",
                                                 suggestion="Proceed and refine with citations.", severity="LOW")],
    )
