---
name: tests-and-ci
description: Add unit tests for message serialization, prompt formatting, and Kafka listener logic, plus a GitHub Actions build+test workflow. Use when writing tests for this repo or setting up .github/workflows CI.
---

# Tests + CI

## Current state (verified by reading the repo)
- The only test file is `src/test/java/com/example/ChatIntegrationZoya/ChatIntegrationZoyaApplicationTests.java` — the default Spring Boot context-load smoke test, nothing else.
- No `.github/workflows` directory exists — zero CI currently.
- `pom.xml` already has `spring-boot-starter-webmvc-test` as a test dependency; will likely also want `spring-kafka-test` (for `@EmbeddedKafka`) depending on how deep the Kafka listener tests go.

## Target: 5-10 tests + CI
Concrete, testable units in this codebase (grounded in actual code, not generic advice):
1. **Payload serialization** — `ChatController.generate` builds `Map.of("message", ..., "requestId", ...)` and serializes via Jackson; `KafkaProcessor.handleUserQuestion` deserializes it back with `objectMapper.readValue(record.value(), Map.class)`. Test the round trip, including the edge case where `message` contains characters that need escaping.
2. **Prompt formatting** — `new Prompt(new UserMessage(messageText))` construction; test that an empty/blank message and a very long message are handled the way you expect (this is currently unguarded — worth deciding on validation while writing the test).
3. **Kafka listener logic** — mock `OpenAiChatModel` and verify `handleUserQuestion` calls `storeResponse` (or, post-split, publishes to `chat-responses`) with the right requestId/answer; verify the exception path logs and triggers the error handler from [[reliability-hardening]] rather than silently swallowing.
4. **Response store** — `ChatController.getResponse` for the "not found yet" (`processing`) vs "found" (`complete`) branches.
5. If time allows: an `@EmbeddedKafka` integration test that exercises the real produce→consume→(post-split) produce-back flow end to end.

## CI checklist
1. `.github/workflows/ci.yml`: trigger on push + PR, `actions/setup-java` (Java 17, matches `pom.xml`'s `<java.version>`), cache `~/.m2`, run `mvn -B test`.
2. Keep it to one job/one Java version — a build matrix is unnecessary scope for this repo and dilutes the "I know what CI is for" signal with noise.
3. Make sure the Groq/OpenAI API key isn't required for the test suite to pass (mock `OpenAiChatModel` rather than hitting the real API in CI) — a workflow that needs real secrets to go green is a portfolio red flag, not a green flag.

## Status
Not started. Best done last, once [[split-services]] and [[reliability-hardening]] have landed — otherwise these tests get rewritten when the module boundaries move.
