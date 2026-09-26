FROM python:3.11-alpine

WORKDIR /app

# Install curl for container healthchecks
RUN apk add --no-cache curl

# Copy web files, server script, and utilities
COPY web/ /app/web/
COPY server.py /app/server.py

# Pre-create /app/data with full permissions for volume mounts on NAS
RUN mkdir -p /app/data && chmod 777 /app/data

# Environment configuration
ENV PORT=8080
ENV HOST=0.0.0.0
ENV DOCKER_ENV=true

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/api/status || exit 1

# Launch the lightweight server with runtime permissions enforcement
CMD ["sh", "-c", "chmod -R 777 /app/data 2>/dev/null || true && python3 /app/server.py"]
