"""LLM gateway abstraction (ADR: provider-agnostic). Ollama for the self-hosted open model;
a deterministic stub when no LLM host is wired, so the whole AI stack works end-to-end offline."""

from __future__ import annotations

import json
import logging

import httpx

from app.core.config import Settings

log = logging.getLogger(__name__)


class LlmError(RuntimeError):
    pass


class LlmGateway:
    """Returns a parsed JSON object from the model given a system + user prompt."""

    def __init__(self, settings: Settings):
        self._s = settings

    def generate_json(self, system: str, user: str) -> dict:
        if self._s.use_ollama:
            return self._ollama_json(system, user)
        raise LlmError("no_llm_configured")  # caller falls back to the stub

    def embed(self, texts: list[str]) -> list[list[float]]:
        """Embed each text via the Ollama embedding model. Raises LlmError if no model is wired."""
        if not self._s.use_ollama:
            raise LlmError("no_llm_configured")
        url = f"{self._s.llm_base_url.rstrip('/')}/api/embeddings"
        out: list[list[float]] = []
        try:
            with httpx.Client(timeout=self._s.llm_timeout_seconds) as client:
                for t in texts:
                    resp = client.post(url, json={"model": self._s.embed_model, "prompt": t})
                    resp.raise_for_status()
                    out.append(resp.json()["embedding"])
            return out
        except (httpx.HTTPError, KeyError) as e:
            log.warning("Ollama embeddings failed: %s", e)
            raise LlmError(str(e)) from e

    def _ollama_json(self, system: str, user: str) -> dict:
        url = f"{self._s.llm_base_url.rstrip('/')}/api/chat"
        payload = {
            "model": self._s.llm_model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            "stream": False,
            "format": "json",
            "keep_alive": "30m",  # keep the model resident so requests don't pay reload cost
            "options": {"temperature": 0.3, "num_predict": self._s.llm_max_tokens},
        }
        try:
            with httpx.Client(timeout=self._s.llm_timeout_seconds) as client:
                resp = client.post(url, json=payload)
                resp.raise_for_status()
                content = resp.json()["message"]["content"]
                return json.loads(content)
        except (httpx.HTTPError, KeyError, json.JSONDecodeError) as e:
            log.warning("Ollama call failed: %s", e)
            raise LlmError(str(e)) from e
