"""Document export (FR-DOC-6/7). The Java backend gathers a document's sections + the template
format rule and posts them here; the worker renders a .docx (python-docx) and, when a Gotenberg
service is wired, a .pdf. The worker is never public — calls carry the shared internal secret."""

from __future__ import annotations

import httpx
from fastapi import APIRouter, Depends, HTTPException, Response, status
from pydantic import BaseModel, ConfigDict, Field

from app.core.config import get_settings
from app.interfaces.routers.ai import require_internal
from app.services.docx_export import build_docx

router = APIRouter(prefix="/export", tags=["export"])


class _Lenient(BaseModel):
    model_config = ConfigDict(extra="ignore")


class ExportSection(_Lenient):
    chapter: str | None = None
    heading: str | None = None
    content: dict | None = None  # ProseMirror/Tiptap JSON doc


class FormatRule(_Lenient):
    font_family: str = "Times New Roman"
    font_size_pt: float = 12
    line_spacing: float = 2.0


class ExportRequest(_Lenient):
    title: str = ""
    format_rule: FormatRule | None = None
    sections: list[ExportSection] = Field(default_factory=list)


def _render_docx(req: ExportRequest) -> bytes:
    return build_docx(
        req.title,
        [s.model_dump() for s in req.sections],
        req.format_rule.model_dump() if req.format_rule else None,
    )


@router.post("/docx", dependencies=[Depends(require_internal)])
def export_docx(req: ExportRequest) -> Response:
    return Response(
        content=_render_docx(req),
        media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )


@router.post("/pdf", dependencies=[Depends(require_internal)])
def export_pdf(req: ExportRequest) -> Response:
    settings = get_settings()
    if not settings.pdf_enabled:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="PDF export is not available (no conversion service configured).",
        )
    docx_bytes = _render_docx(req)
    try:
        with httpx.Client(timeout=120.0) as client:
            resp = client.post(
                f"{settings.gotenberg_url.rstrip('/')}/forms/libreoffice/convert",
                files={"files": ("document.docx", docx_bytes,
                                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document")},
            )
            resp.raise_for_status()
            pdf_bytes = resp.content
    except httpx.HTTPError as exc:  # noqa: BLE001 - surface as 503 to the backend
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"PDF conversion failed: {exc}",
        ) from exc
    return Response(content=pdf_bytes, media_type="application/pdf")
