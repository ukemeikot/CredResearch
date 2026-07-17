FROM node:20-slim
WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY . .

# Non-root (defense-in-depth; Trivy DS-0002).
RUN useradd -r -u 10001 appuser
USER appuser

EXPOSE 1234
CMD ["node", "src/server.js"]
