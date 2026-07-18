"""Descriptive data analysis endpoint (Phase 8). The backend forwards CSV bytes; the worker returns
type/missing detection + descriptive stats + chart-ready distributions. Internal only."""

from fastapi import APIRouter, Depends, Request

from app.interfaces.routers.ai import require_internal
from app.services.analyze import analyze_csv

router = APIRouter(prefix="/analysis", tags=["analysis"])


@router.post("/describe", dependencies=[Depends(require_internal)])
async def describe(request: Request) -> dict:
    data = await request.body()
    return analyze_csv(data)
