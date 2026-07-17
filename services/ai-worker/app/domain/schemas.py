"""AI feature request/response contracts (Phase 4). Outputs are validated JSON (FR-AI).

Response models are intentionally lenient (extra fields ignored, generous defaults, string enums
normalised) so a small self-hosted model's slightly-off JSON is accepted and coerced rather than
rejected into the stub. Requests stay strict."""

from pydantic import BaseModel, ConfigDict, Field, field_validator

# Accepted as free text but normalised to LOW/MEDIUM/HIGH; unknown values fall back to MEDIUM.
Feasibility = str
Severity = str


def _norm_level(v: object) -> str:
    s = str(v).strip().upper()
    return s if s in {"LOW", "MEDIUM", "HIGH"} else "MEDIUM"


class _Lenient(BaseModel):
    """Base for AI outputs: ignore unexpected fields the model may add."""

    model_config = ConfigDict(extra="ignore")


# ── Topic generator + feasibility ────────────────────────────────────────────
class TopicRequest(BaseModel):
    field: str
    interests: str = ""
    level: str = "UG"  # UG | MSc | PhD


class TopicIdea(_Lenient):
    title: str = ""
    rationale: str = ""
    feasibility: Feasibility = "MEDIUM"
    suggested_methods: list[str] = Field(default_factory=list)

    _nf = field_validator("feasibility", mode="before")(lambda v: _norm_level(v))


class TopicResponse(_Lenient):
    topics: list[TopicIdea] = Field(default_factory=list)


# ── Objectives / research questions / hypotheses ─────────────────────────────
class ObjectivesRequest(BaseModel):
    topic: str
    problem: str = ""
    level: str = "UG"


class ObjectivesResponse(_Lenient):
    aim: str = ""
    objectives: list[str] = Field(default_factory=list)
    research_questions: list[str] = Field(default_factory=list)
    hypotheses: list[str] = Field(default_factory=list)


# ── Problem statement refinement ─────────────────────────────────────────────
class ProblemStatementRequest(BaseModel):
    topic: str
    context: str = ""


class ProblemStatementResponse(_Lenient):
    problem_statement: str = ""
    significance: str = ""


# ── Section assistant (proposal / methodology / general drafting) ────────────
class SectionAssistRequest(BaseModel):
    heading: str
    guidance: str = ""
    current_text: str = ""
    instruction: str = "Draft or improve this section."


class SectionAssistResponse(_Lenient):
    suggestion: str = ""
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


class AlignmentFinding(_Lenient):
    area: str = ""
    issue: str = ""
    suggestion: str = ""
    severity: Severity = "MEDIUM"

    _ns = field_validator("severity", mode="before")(lambda v: _norm_level(v))


class AlignmentResponse(_Lenient):
    overall_score: int = 60
    summary: str = ""
    findings: list[AlignmentFinding] = Field(default_factory=list)

    @field_validator("overall_score", mode="before")
    @classmethod
    def _clamp(cls, v: object) -> int:
        try:
            return max(0, min(100, int(float(v))))
        except (TypeError, ValueError):
            return 60


# ── Paper summarization (Phase 5, FR-LIT-4) ──────────────────────────────────
class SummaryRequest(BaseModel):
    text: str = ""


class SummaryResponse(_Lenient):
    summary: str = ""
    methodology: str = ""
    findings: list[str] = Field(default_factory=list)
    limitations: list[str] = Field(default_factory=list)
    gaps: list[str] = Field(default_factory=list)
