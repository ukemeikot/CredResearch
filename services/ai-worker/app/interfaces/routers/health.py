from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict[str, str]:
    """Liveness probe used by Docker and the backend's degradation checks."""
    return {"status": "ok", "service": "ai-worker"}
