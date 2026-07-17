"""Paper extraction endpoint (Phase 5). The Java backend forwards uploaded file bytes here (raw
body + X-Filename header) and receives extracted text + bibliographic metadata as JSON. Internal
only — guarded by the shared secret."""

from fastapi import APIRouter, Depends, Header, Request

from app.interfaces.routers.ai import require_internal
from app.services.extract import extract_paper

router = APIRouter(prefix="/papers", tags=["papers"])


@router.post("/extract", dependencies=[Depends(require_internal)])
async def extract(request: Request, x_filename: str = Header(default="upload.pdf")) -> dict:
    data = await request.body()
    return extract_paper(x_filename, data)
