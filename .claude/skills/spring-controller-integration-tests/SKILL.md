---
name: spring-controller-integration-tests
description: Write focused integration tests for Spring Boot WebFlux controller endpoints. Use when adding or updating controller integration tests covering standard success flows and important error scenarios, while reporting API defects without modifying production code.
---

# Controller integration tests

## Workflow

1. Inspect the target controller, related business rules, security
   configuration, exception handlers, and nearby integration tests.
2. Identify the standard successful flow and important error scenarios.
3. Implement the smallest useful set of tests using existing conventions.
4. Run the relevant tests.
5. Report coverage, results, and discovered API issues.

## Scope

- Cover the standard successful flow for each requested action.
- Cover relevant errors such as invalid input, resource not found,
  unauthenticated access, insufficient permissions, and business conflicts.
- Use one representative test per distinct relevant error.
- Avoid exhaustive field validation, input combinations, and edge cases.
- Stay within the requested controllers or actions.

## Test implementation

- Exercise endpoints through WebTestClient.
- Use real internal service and persistence layers.
- Follow the existing integration-test setup and authentication helpers.
- Mock external systems only when necessary.
- Create only the data and authentication needed for each scenario.
- Keep tests readable, deterministic, and isolated.
- Reuse existing fixtures and helpers.
- Avoid unnecessary abstractions.

## Assertions

- Assert the expected HTTP status.
- Assert only response properties relevant to the scenario.
- Prefer stable error codes or field identifiers for error responses.
- Avoid exact error-message assertions unless wording is contractual.
- Avoid full-response equality, snapshots, and unrelated field assertions.
- Do not assert exact generated IDs or timestamps.
- Verify essential persisted effects when the response cannot prove success.
- For rejected operations, verify unchanged state where preventing a
  side effect is important.

## Production code and API defects

- Do not modify production code or production configuration.
- Report abnormal behavior or suspected misconfiguration involving:
  parameter binding, authentication, authorization, request bodies,
  validation, response serialization, or HTTP status mapping.
- Do not weaken assertions, bypass security, or accept incorrect behavior
  merely to make tests pass.
- When intended behavior is clear, retain a focused failing test that
  demonstrates the defect.
- When intended behavior is ambiguous, report the ambiguity rather than
  inventing an API contract.
- Continue testing unaffected scenarios.
- Distinguish API defects from test setup and environment failures.

## Final report

Summarize:
- Scenarios covered.
- Tests passed, failed, or not run.
- Discovered issues, each with the request/scenario, expected behavior,
  actual behavior, and supporting evidence.
- Likely causes only when supported by evidence.
