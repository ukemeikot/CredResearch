"""Internal similarity check endpoint (Phase 9, FR-SIM). Backend sends the target text + the user's
own corpus; the worker returns matched paragraphs + citation-risk flags. Internal only."""

from fastapi import APIRouter, Depends
from pydantic import BaseModel, ConfigDict, Field

from app.interfaces.routers.ai import require_internal
from app.services.similarity import check

router = APIRouter(prefix="/similarity", tags=["similarity"])


class _Lenient(BaseModel):
    model_config = ConfigDict(extra="ignore")


class SimSource(_Lenient):
    id: str = ""
    title: str = "Untitled"
    text: str = ""


class SimRequest(_Lenient):
    target_text: str = ""
    sources: list[SimSource] = Field(default_factory=list)


@router.post("/check", dependencies=[Depends(require_internal)])
def similarity_check(req: SimRequest) -> dict:
    return check(req.target_text, [s.model_dump() for s in req.sources])
