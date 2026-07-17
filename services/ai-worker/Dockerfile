FROM python:3.11-slim
WORKDIR /app

# Install runtime deps first for layer caching.
COPY pyproject.toml ./
RUN pip install --no-cache-dir \
    "fastapi>=0.115" "uvicorn[standard]>=0.30" "pydantic>=2.7" "pydantic-settings>=2.3" "httpx>=0.27" "python-docx>=1.1" "pypdf>=5.1"

COPY . .
# Run as a non-root user (defense-in-depth; Trivy DS-0002).
RUN useradd -r -u 10001 appuser
USER appuser
EXPOSE 8001
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
