---
name: spring-unit-tests
description: Write focused unit tests for Kotlin and Java Spring Boot applications, including WebFlux and coroutine-based services. Use when adding or updating unit tests for business logic, standard successful flows, and important error scenarios without starting Spring or modifying production code.
---

# Unit tests

## In this repository

- In `controller/`, only authorization classes (`controller/<resource>/authorization/`) get unit
  tests. Operations, resolvers, commands, filters and responses are covered by integration tests;
  use the `spring-controller-integration-tests` skill for them. If asked to unit test one of those,
  say so and ask before writing it.
- Outside `controller/`, unit test classes with their own logic, such as `security/` classes.
- Test classes mirror the main package: `src/test/kotlin/io/kostack/skeleton/<same package>/<Class>Test.kt`.
- Tools: JUnit 5, MockK (`mockk`, `coEvery`, `coVerify`), `runTest`, and `StepVerifier` or
  `kotlinx.coroutines.reactor` helpers for `Mono` results.

## Workflow

1. Inspect the target class, its dependencies, business rules, and nearby tests.
2. Identify the standard successful flow and important failure scenarios.
3. Implement the smallest useful set of unit tests.
4. Run the relevant tests.
5. Report coverage, results, and discovered defects.

## Scope

- Test a cohesive unit of business behavior, which may include several
  real collaborating classes.
- Cover standard successful behavior and important error scenarios.
- Include boundary cases only when they represent meaningful business rules.
- Avoid exhaustive input combinations, trivial getter/setter tests,
  and tests that merely reproduce the implementation.
- Stay within the requested classes or methods.

## Test implementation

- Follow existing test frameworks, naming, and project conventions.
- Instantiate the class directly; do not start a Spring context.
- Do not use @SpringBootTest, slice-test annotations, or Testcontainers.
- Mock external dependencies using the project's existing mocking library.
- Use real value objects, DTOs, and domain entities where practical.
- Do not mock the class under test, private methods, or internal details.
- Stub only dependency behavior needed for the scenario.
- Keep setup minimal and tests independent.
- Reuse existing fixtures when helpful; avoid unnecessary abstractions.
- Use descriptive test names expressing behavior and expected outcomes.

## Dependencies and test boundaries

- Prefer real objects and lightweight real collaborators when
  they are deterministic and easy to construct.
- Mock only when necessary to isolate external I/O, control a failure
  scenario, or avoid expensive or complex infrastructure setup.
- If mocking many dependencies makes the test harder to understand,
  use real collaborators and mock at the external boundaries.
- Do not recursively build a large dependency graph merely to avoid mocks.
- Keep databases, network clients, brokers, and other external systems mocked.
- Instantiate classes directly without starting a Spring context.
- Allow a small group of collaborating classes to form the unit under test.
  Assert their observable behavior rather than internal interactions.
- Follow existing mocking conventions and stub only what the scenario needs.
- Do not mock the class under test or its private methods.

## Assertions

- Assert the result or state change that proves the intended behavior.
- Assert only properties relevant to the scenario.
- Avoid full-object equality when unrelated fields make the test brittle.
- For failures, assert the exception type and relevant stable error code.
- Assert exact exception messages only when wording is contractual.
- Verify dependency calls when the interaction itself is important:
  persistence, event publication, notifications, or preventing side effects.
- Verify essential arguments, not every incidental invocation.
- Avoid blanket verifyNoMoreInteractions and strict call ordering unless
  required by the behavior.

## Coroutines and reactive code

- Use runTest for suspending functions when compatible with the project.
- Use coroutine-aware stubbing and verification for suspend dependencies.
- Use StepVerifier for Mono and Flux when testing reactive behavior.
- Subscribe to reactive results so the tested logic actually executes.
- Cover empty results or error signals when meaningful to the contract.
- Avoid Thread.sleep and real waiting.
- Use virtual time where appropriate for delays, retries, or timeouts.
- Do not assume coroutine virtual time controls Reactor schedulers.
- Bound asynchronous verification so failures cannot hang the test suite.
- Test cancellation or backpressure only when relevant to the unit.

## Determinism

- Avoid real network, database, filesystem, or broker dependencies.
- Use fixed inputs and deterministic test data.
- Control time and randomness through existing injectable dependencies.
- If the design prevents isolation, report the limitation rather than
  modifying production code or adding invasive mocking workarounds.

## Production code and defects

- Do not modify production code or production configuration.
- Report incorrect behavior, suspicious logic, and relevant misconfiguration.
- Do not weaken assertions or change expected behavior merely to pass tests.
- When intended behavior is clear, retain a focused failing test that
  demonstrates the defect.
- When behavior is ambiguous, report the uncertainty rather than inventing
  a requirement.
- Continue testing unaffected scenarios.
- Distinguish production defects from test setup or environment failures.

## Final report

Summarize:
- Behaviors covered.
- Tests passed, failed, or not run.
- Discovered defects with expected behavior, actual behavior, and evidence.
- Any limitations that require integration tests or clarification.

Do not claim unit tests verify Spring wiring, HTTP binding, security filters,
database behavior, or framework-managed transactions.
