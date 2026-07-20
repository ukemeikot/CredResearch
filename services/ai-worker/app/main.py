from fastapi import FastAPI

from app.interfaces.routers import ai, analysis, export, health, papers, similarity


def create_app() -> FastAPI:
    """Application factory. Inbound adapters (routers) are wired here; use cases and
    domain logic stay framework-agnostic per app/ARCHITECTURE.md."""
    app = FastAPI(title="CredResearch AI Worker", version="0.1.0")
    app.include_router(health.router)
    app.include_router(ai.router)
    app.include_router(export.router)
    app.include_router(papers.router)
    app.include_router(analysis.router)
    app.include_router(similarity.router)
    return app


app = create_app()
