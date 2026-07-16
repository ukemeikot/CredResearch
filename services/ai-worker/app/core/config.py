"""Runtime configuration (12-factor: all from env). See app/ARCHITECTURE.md."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="", case_sensitive=False)

    # LLM gateway. Provider "ollama" calls a self-hosted Ollama server; "stub" returns
    # deterministic, schema-valid responses (used when no LLM host is wired yet).
    llm_provider: str = "stub"          # ollama | stub
    llm_base_url: str = ""              # e.g. http://10.x.x.x:11434 (Ollama)
    llm_model: str = "llama3.2:3b"
    llm_timeout_seconds: float = 180.0
    llm_max_tokens: int = 768  # cap generation length so CPU inference stays bounded

    # Shared secret the Java backend presents on every worker call (the worker is never
    # exposed publicly). Empty disables the check (local dev only).
    internal_service_secret: str = ""

    @property
    def use_ollama(self) -> bool:
        return self.llm_provider.lower() == "ollama" and bool(self.llm_base_url)


@lru_cache
def get_settings() -> Settings:
    return Settings()
