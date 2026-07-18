"""Phase 4 AI endpoints. The worker is never public — the Java backend calls it with a shared
secret (X-Internal-Secret). All outputs are validated JSON schemas."""

from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.core.config import get_settings
from app.domain import schemas as s
from app.services import ai_features

router = APIRouter(prefix="/ai", tags=["ai"])


def require_internal(x_internal_secret: str | None = Header(default=None)) -> None:
    secret = get_settings().internal_service_secret
    if secret and x_internal_secret != secret:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="internal secret required")


@router.post("/topics", response_model=s.TopicResponse, dependencies=[Depends(require_internal)])
def topics(req: s.TopicRequest) -> s.TopicResponse:
    return ai_features.topics(req)


@router.post("/objectives", response_model=s.ObjectivesResponse, dependencies=[Depends(require_internal)])
def objectives(req: s.ObjectivesRequest) -> s.ObjectivesResponse:
    return ai_features.objectives(req)


@router.post("/problem-statement", response_model=s.ProblemStatementResponse, dependencies=[Depends(require_internal)])
def problem_statement(req: s.ProblemStatementRequest) -> s.ProblemStatementResponse:
    return ai_features.problem_statement(req)


@router.post("/section-assist", response_model=s.SectionAssistResponse, dependencies=[Depends(require_internal)])
def section_assist(req: s.SectionAssistRequest) -> s.SectionAssistResponse:
    return ai_features.section_assist(req)


@router.post("/alignment", response_model=s.AlignmentResponse, dependencies=[Depends(require_internal)])
def alignment(req: s.AlignmentRequest) -> s.AlignmentResponse:
    return ai_features.alignment(req)


@router.post("/summarize", response_model=s.SummaryResponse, dependencies=[Depends(require_internal)])
def summarize(req: s.SummaryRequest) -> s.SummaryResponse:
    return ai_features.summarize(req)


@router.post("/embed", response_model=s.EmbedResponse, dependencies=[Depends(require_internal)])
def embed(req: s.EmbedRequest) -> s.EmbedResponse:
    return ai_features.embed(req)


@router.post("/rag-answer", response_model=s.RagAnswerResponse, dependencies=[Depends(require_internal)])
def rag_answer(req: s.RagAnswerRequest) -> s.RagAnswerResponse:
    return ai_features.rag_answer(req)


@router.post("/questionnaire", response_model=s.QuestionnaireGenResponse, dependencies=[Depends(require_internal)])
def questionnaire(req: s.QuestionnaireGenRequest) -> s.QuestionnaireGenResponse:
    return ai_features.generate_questionnaire(req)


@router.post("/interpret-data", response_model=s.InterpretResponse, dependencies=[Depends(require_internal)])
def interpret_data(req: s.InterpretRequest) -> s.InterpretResponse:
    return ai_features.interpret_data(req)


@router.post("/chapter4", response_model=s.Chapter4Response, dependencies=[Depends(require_internal)])
def chapter4(req: s.InterpretRequest) -> s.Chapter4Response:
    return ai_features.chapter4(req)
