---
name: reliability-hardening
description: Add DLQ, retry with backoff, correlation-id structured logging, and basic metrics to the Kafka consumer path. Use when working on error handling, KafkaListener resilience, logging, or Micrometer metrics for worker-service.
---

# Reliability hardening (the "data-engineer-ish" pass)

## Current state (verified by reading the repo)
- `KafkaProcessor.handleUserQuestion` has **zero retry/backoff config**. On any exception (bad JSON, model call failure, anything) it logs and rethrows a bare `RuntimeException`. With Spring Kafka's default error handling this either retries indefinitely with no backoff or eventually just drops the record, depending on container factory defaults — worth checking exactly what happens before "fixing" it, since the current default container factory in `KafkaConfig.java` doesn't set a `CommonErrorHandler` at all.
- No dead-letter topic exists.
- `requestId` already exists end-to-end (generated in `ChatController.generate`, threaded through the Kafka payload, logged in `KafkaProcessor`) — it's just not in MDC, so it doesn't automatically tag every log line, only the ones that explicitly interpolate it.
- No metrics of any kind currently (no Micrometer/Actuator dependency in `pom.xml`).

## Target: pick 3 of these (not all 5 — scope it deliberately)
1. **DLQ topic** (`user-questions-dlq`) via `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` on the consumer factory.
2. **Retry with backoff** — `ExponentialBackOff` on the same `DefaultErrorHandler`, so transient LLM API failures get a few retries before hitting the DLQ.
3. **Correlation id / structured logging** — put `requestId` into MDC (`MDC.put("requestId", requestId)`) at the top of the listener, configure the log pattern to include it, clear it in a `finally`.
4. **Basic metrics** — Micrometer `Counter` for jobs processed/failed, `Timer` around the `chatModel.call(prompt)` call. Expose via Actuator if there's time to also stand up Prometheus/Grafana; otherwise metrics-in-code is still a legitimate resume line even without the dashboard.
5. *(stretch, skip unless time allows)* **Idempotency key** — dedupe on `requestId` so a redelivered message (e.g. after a retry that actually succeeded but crashed before offset commit) doesn't produce two Groq calls.

Recommended pick: 1, 2, 3 as the core three; add 4 if time allows; treat 5 as optional.

## Checklist
1. Confirm current default error-handling behavior empirically (don't assume) before layering retry/DLQ on top — write a quick failing-message test first.
2. Add `DefaultErrorHandler` bean wired into `kafkaListenerContainerFactory` with backoff + DLQ recoverer.
3. Add MDC correlation-id logging, update `logback`/log pattern if needed.
4. If doing metrics: add `spring-boot-starter-actuator` + micrometer registry, instrument the listener.

## Status
Not started. Depends on [[split-services]] existing (this work targets worker-service's consumer specifically) — can be done against the single-module version too if the split is delayed, but the listener code will move either way.
