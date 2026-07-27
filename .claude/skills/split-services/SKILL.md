---
name: split-services
description: Split the single ChatIntegrationZoya Spring Boot module into api-service and worker-service (plus optional result-store), communicating only through Kafka topics. Use when restructuring modules, moving ChatController/KafkaProcessor apart, or deciding on Eureka vs Compose networking.
---

# Split into api-service / worker-service

## Current state (verified by reading the repo)
- Everything lives in one module: `ChatController` (REST endpoints + Kafka producer + in-memory `ConcurrentHashMap` response store) and `KafkaProcessor` (`@KafkaListener` + Spring AI call) are beans in the same JVM.
- **Important finding**: the Kafka round trip is currently half-fake. `KafkaProcessor.handleUserQuestion` calls `chatController.storeResponse(requestId, aiAnswer)` as a **direct in-process method call** on the injected bean — it never publishes a result message to a topic. So today, Kafka only carries the request (`user-questions` topic); the response path is plain Java, not Kafka. This only works because producer and consumer are colocated in one process. The moment these split into two services, this direct call becomes impossible and must become a real second topic.
- `ChatController.getResponse` polls the in-memory map. The frontend (`spring-frontend/src/components/Chatbot.js`) already polls `/ai/response/{requestId}` every 2s client-side — that contract doesn't need to change, only what's behind it.

## Target end state
- **api-service**: exposes `/ai/generate` and `/ai/response/{requestId}`, publishes to `user-questions`, and has its own `@KafkaListener` on a new `chat-responses` topic that populates its response store. No direct calls to worker code.
- **worker-service**: consumes `user-questions`, calls the Spring AI / OpenAI(Groq) model, publishes the result to `chat-responses`. No HTTP surface, no knowledge of the frontend.
- **(optional) result-store**: Postgres or Redis backing the response store instead of `ConcurrentHashMap`, so state survives an api-service restart and multiple api-service replicas can share it. Decide based on time budget — this is the most skippable of the three.
- Service discovery: **skip Eureka deliberately**. Use Docker Compose service DNS names (`worker-service`, `api-service` as hostnames) + Kafka bootstrap-servers env var. Note the Eureka/Kubernetes tradeoff explicitly in the README as considered-and-deferred, not unknown.

## Checklist
1. Define the wire contract first: topic names (`user-questions`, `chat-responses`), payload shape (requestId, message/answer, status) — small shared DTO or just documented JSON shape, don't over-engineer a shared library for a 2-service demo.
2. Create `api-service` module: move `ChatController`, add a new `@KafkaListener` for `chat-responses` that replaces the direct `storeResponse` call.
3. Create `worker-service` module: move `KafkaProcessor`, `KafkaConfig` (or a trimmed version), Spring AI dependency. Change it to publish to `chat-responses` instead of calling `chatController` directly.
4. Update `docker-compose.yml` (from [[containerize-stack]]) to run both services against the same Kafka broker.
5. Decide on result-store; implement only if the two-service split is solid and tested first.
6. Update README with the architecture diagram and the Eureka-vs-Compose-networking tradeoff writeup — this is a real interview talking point, make it visible.

## Status
Not started. Depends on [[containerize-stack]] being functional enough to test against (need a working Kafka broker to verify the new `chat-responses` round trip).
