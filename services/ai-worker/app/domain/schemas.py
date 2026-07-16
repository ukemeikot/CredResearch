"""AI feature request/response contracts (Phase 4). Outputs are validated JSON (FR-AI)."""

from typing import Literal

from pydantic import BaseModel, Field

Feasibility = Literal["LOW", "MEDIUM", "HIGH"]
Severity = Literal["LOW", "MEDIUM", "HIGH"]


# ── Topic generator + feasibility ────────────────────────────────────────────
class TopicRequest(BaseModel):
    field: str
    interests: str = ""
    level: str = "UG"  # UG | MSc | PhD


class TopicIdea(BaseModel):
    title: str
    rationale: str
    feasibility: Feasibility = "MEDIUM"
    suggested_methods: list[str] = Field(default_factory=list)


class TopicResponse(BaseModel):
    topics: list[TopicIdea]


# ── Objectives / research questions / hypotheses ─────────────────────────────
class ObjectivesRequest(BaseModel):
    topic: str
    problem: str = ""
    level: str = "UG"


class ObjectivesResponse(BaseModel):
    aim: str
    objectives: list[str]
    research_questions: list[str]
    hypotheses: list[str] = Field(default_factory=list)


# ── Problem statement refinement ─────────────────────────────────────────────
class ProblemStatementRequest(BaseModel):
    topic: str
    context: str = ""


class ProblemStatementResponse(BaseModel):
    problem_statement: str
    significance: str


# ── Section assistant (proposal / methodology / general drafting) ────────────
class SectionAssistRequest(BaseModel):
    heading: str
    guidance: str = ""
    current_text: str = ""
    instruction: str = "Draft or improve this section."


class SectionAssistResponse(BaseModel):
    suggestion: str
    notes: list[str] = Field(default_factory=list)


# ── Research alignment engine ────────────────────────────────────────────────
class AlignmentSection(BaseModel):
    heading: str
    text: str = ""


class AlignmentRequest(BaseModel):
    title: str
    abstract: str = ""
    objectives: list[str] = Field(default_factory=list)
    sections: list[AlignmentSection] = Field(default_factory=list)


class AlignmentFinding(BaseModel):
    area: str
    issue: str
    suggestion: str
    severity: Severity = "MEDIUM"


class AlignmentResponse(BaseModel):
    overall_score: int = Field(ge=0, le=100)
    summary: str
    findings: list[AlignmentFinding]
