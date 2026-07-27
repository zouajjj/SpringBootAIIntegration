---
name: containerize-stack
description: Make the ChatIntegrationZoya stack (Kafka + Spring Boot app + optionally the CRA frontend) boot for real with `docker compose up`. Use when touching docker-compose.yml, writing the app's Dockerfile, or externalizing Kafka bootstrap config.
---

# Containerize the stack for real

## Current state (verified by reading the repo)
- `docker-compose.yml` only defines a bare `apache/kafka:latest` service with a port mapping. No KRaft env vars set — `KAFKA_NODE_ID`, `KAFKA_PROCESS_ROLES`, `KAFKA_CONTROLLER_QUORUM_VOTERS`, `KAFKA_LISTENERS`/`KAFKA_ADVERTISED_LISTENERS`, `CLUSTER_ID` are all missing. This image will not reliably start in KRaft mode without them, and even if it does, other containers can't reach it because there's no internal listener distinct from the host-facing one.
- No Dockerfile for the Spring Boot app exists yet.
- `localhost:9092` is hardcoded in **two places**: `src/main/java/com/example/ChatIntegrationZoya/KafkaConfig.java` (producer/consumer factories build their own config maps) and `src/main/resources/application.properties` (`spring.kafka.bootstrap-servers`). These need to collapse to a single externalized source before a container network can work — right now KafkaConfig.java's hardcoded maps would win regardless of what's in properties or env.
- `application.properties` has `spring.ai.openai.api-key=${{ secrets.GROQ}}` — this is GitHub Actions template syntax, not valid Spring property placeholder syntax (`${GROQ_API_KEY}` is what Spring expects). This is a live bug, not just a container-config issue.
- The frontend (`spring-frontend/`, CRA) already has a `build` script that copies its build output into `src/main/resources/static` via `cpy` — meaning the intended deployment shape is "frontend gets baked into the Spring Boot jar as static resources," not served as its own process. Decide deliberately whether to keep that (simpler, one container) or run it as a second container (nicer for hot-reload demos, more "microservices" optics for a portfolio).

## Target end state
- `docker compose up` from a clean checkout, with Docker running, produces a working stack: Kafka (properly configured, KRaft or zk — pick one and document why), the Spring Boot app(s), optionally the frontend — no manual steps beyond providing a `.env` for secrets.
- Kafka bootstrap servers and the OpenAI/Groq API key are both sourced from environment variables / a gitignored `.env` file, never hardcoded.
- `depends_on` uses `condition: service_healthy` (with a real healthcheck on the Kafka container) so the app doesn't race Kafka on startup.

## Checklist
1. Decide Kafka image approach: fix `apache/kafka:latest` KRaft env vars directly, or switch to `confluentinc/cp-kafka` (+ zookeeper) for a config shape more people recognize in interviews. Pick one, don't do both.
2. Add a multi-stage Dockerfile for the Spring Boot app (maven build stage → slim JRE runtime stage).
3. Fix `KafkaConfig.java` to read bootstrap servers from `@Value("${spring.kafka.bootstrap-servers}")` (or drop the manually-built factories entirely and rely on Spring Boot's kafka auto-configuration from properties) instead of hardcoding `localhost:9092`.
4. Fix the broken secret placeholder: `spring.ai.openai.api-key=${GROQ_API_KEY}`, sourced via `.env` (gitignored) in compose.
5. Add healthchecks to the Kafka service and `depends_on: condition: service_healthy` on the app service(s).
6. Decide + implement the frontend approach (baked into the jar vs its own container) and document the decision in the README.

## Status
Not started. Depends on Docker + Kafka being available locally (user runs these manually before we start).
