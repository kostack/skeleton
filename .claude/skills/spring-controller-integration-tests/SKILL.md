---
name: spring-controller-integration-tests
description: Write focused integration tests for Spring Boot WebFlux endpoints (operation classes or controllers). Use when adding or updating endpoint integration tests covering standard success flows, business rules, and important error scenarios, while reporting API defects without modifying production code.
---

# Endpoint integration tests

## Project conventions

Read the project's `CLAUDE.md` first: it names the integration test package, the base test class and
its helpers (authentication, fixture references, concurrent requests), and the reference tests to
copy. Where this skill and `CLAUDE.md` disagree, `CLAUDE.md` wins.

- When endpoints are operation classes (one class per endpoint with a single
  `suspend operator fun invoke` handler), write one test class per operation:
  `<Operation>IntegrationTest` (for example `CreateXxxIntegrationTest` for `CreateXxx`), mirroring the
  operation's package under the integration test package.
- Extend the project's base integration test class, authenticate through its helpers, read fixture
  data through its fixture references, and check persisted state with `@Autowired` repositories.
- Document the fixture data the class relies on in its KDoc.
- When operations and resolvers have no unit tests, these tests also cover their behavior: business
  rules (validation, duplicate handling, idempotent transitions, concurrency where it matters) and
  path-entity resolution (unknown entities return 404).
- Access rules: a creator-private resource resolves only for its owner, so another user's entity
  returns 404. A `@PreAuthorize` owner rule returns 403. An owner-scoped list must not return other
  users' entities, including through its filters.
- Fixture references are snapshots taken when the fixture stored them. When a fixture updates an
  entity after storing its reference, read that field from the repository instead of the reference.
- PUT updates replace the resource: send every required field in each request, and test that an
  omitted or `null` optional field or reference is cleared. Test the omitted and the explicit `null`
  cases separately; a raw JSON string (`"""{"field": null}"""`) makes the explicit `null` obvious.
- Related ids in request bodies: test that an unknown id returns 400 with a violation on that
  `fieldId`, and that nothing was persisted.
- For list endpoints with several filters, a small `list(vararg params)` helper keeps each test
  focused on its query. Test each filter alone and combined. For a list scoped to a required parent,
  test a missing parent id (400) and an unknown one (404).
- Deletes with children: assert the entity and all its children are gone, and that other parents'
  children remain. For rejected deletes (403, 404, 401), assert nothing was removed.
- Races: when an operation guards against a concurrent one (a delete racing child creates, two
  deletes of the same entity), fire them together with the base class's concurrency helper and
  assert outcomes, not ordering: every status is in the allowed set (for example 204, 201 or 400;
  204 or 404), at least one request reached the goal, no 5xx occurred, and no orphan rows exist.
  These tests can't force an interleaving, so rerun them a few times (with Gradle,
  `./gradlew test --rerun --tests '<Class>'`; without `--rerun` an up-to-date task is skipped).

## Workflow

1. Inspect the target operation or controller, related business rules, security
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
- Stay within the requested operations or actions.

## Test implementation

- Exercise endpoints through WebTestClient.
- Use the real persistence layer (repositories and database).
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
