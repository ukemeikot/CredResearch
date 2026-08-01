"""Document import endpoint. The Java backend forwards an uploaded .docx/.pdf (raw body +
X-Filename header) and receives a structured ProseMirror document (sections + content) to
instantiate as an editable document. Internal only — guarded by the shared secret."""

from fastapi import APIRouter, Depends, Header, HTTPException, Request

from app.interfaces.routers.ai import require_internal
from app.services.import_doc import import_document

router = APIRouter(prefix="/import", tags=["import"])


@router.post("/document", dependencies=[Depends(require_internal)])
async def import_doc(request: Request, x_filename: str = Header(default="upload.docx")) -> dict:
    data = await request.body()
    try:
        return import_document(x_filename, data)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
