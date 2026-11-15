# Skeleton Service – agent guide

Kotlin + Spring Boot (WebFlux, coroutines) service. Package root: `io.kostack.skeleton`.
README.MD has setup, profiles, and Docker details; this file holds the coding conventions.

## Build and verify

- `./gradlew test`: full suite. Integration tests use Testcontainers, so Docker must be running.
- `./gradlew check`: Spotless, then tests, then JaCoCo verification (minimum **80%** coverage;
  `command`, `configuration`, `fixture` and `**/generated/**` are excluded).
- `./gradlew spotlessApply`: fix formatting. Run it before reporting a task done.
- Build logic comes from the kostack gradle-conventions plugins. Keep `build.gradle.kts` limited to
  what is specific to this service.

## Layout

```
controller/<resource>/          <Resource>Controller.kt
  request/                      <Resource>CreateRequest, ... (input DTOs with jakarta validation)
  response/                     <Resource>Response, <Resource>ListResponse, ResponseExtensions.kt
  filter/                       <Resource>ListFilter (query/sort definition for list endpoints)
  resolver/                     <Resource>ArgumentResolver (loads path entities)
  authorization/                <Resource>Authorization (@Component("<resource>Auth"))
service/<resource>/             <Resource>Service (interface) + <Resource>ServiceImpl (@Service)
db/r2dbc/{model,repository}     MySQL entities (@Table) and CoroutineCrudRepository
db/mongodb/{model,repository}   Mongo documents (@Document) and CoroutineCrudRepository
fixture/{r2dbc,mongodb}         AbstractFixture subclasses, ordered with @Order
resources/db/migration          Flyway: V<n>__<description>.sql
http/                           IntelliJ HTTP client files for manual calls
```

Reference implementation for everything: the `todo` resource. When in doubt, copy its shape.

## Kotlin style

- Follow `.editorconfig`: 2-space indent, 120-column lines, no wildcard imports, no trailing commas.
- Use data classes for models and DTOs. Use expression bodies when they fit.
- Everything is non-blocking. Use `suspend` functions and `CoroutineCrudRepository`. Never use
  `.block()`, `runBlocking`, JPA/JDBC repositories, or `Thread.sleep` (startup code in
  `configuration/` is the only exception). Where Spring requires a `Mono`
  (resolvers, `@PreAuthorize` beans), wrap the code with `kotlinx.coroutines.reactor.mono { }`.
- Don't add a custom Jackson `ObjectMapper` configuration. It was removed on purpose.

## API conventions

- Base path `@RequestMapping("/api/{version}/<plural>")`. Every mapping declares `version = "1+"`
  (or a range when a version changes behavior).
- Handlers are `suspend fun <verb>Action(...)`, for example `listAction` or `createAction`.
- Add only the actions that were requested by name. "Create the X controller" means the class with
  no actions. Never add CRUD actions (or their DTOs, resolvers and tests) that nobody asked for.
- Every endpoint has `@Operation(operationId = "<resource>_<verb>_action", summary = "...")`.
  The class has `@Tag`.
- Declare `produces`/`consumes = [MediaType.APPLICATION_JSON_VALUE]` explicitly.
- Status codes: create → `@ResponseStatus(HttpStatus.CREATED)`; update or delete without a body →
  `@ResponseStatus(HttpStatus.NO_CONTENT)`.
- Path entities: take `@PathVariable <resource>Id: String` plus
  `@Parameter(hidden = true) @ResolvedArgument entity: Entity`. The resolver returns 404 through
  `ResponseStatusException(HttpStatus.NOT_FOUND)`. Controllers never load entities themselves.
- Authorization is per endpoint, as needed. When access is restricted (for example owner-only), put it
  on the method: `@PreAuthorize("@<resource>Auth.requireOwner(#entity, authentication.principal)")`.
- Get the current user with `@AuthenticationPrincipal user: User` (the Mongo `User`).
- Response DTOs are optional. Returning an entity directly is fine (see `MeController` returning
  `User`); hide internal fields with `@JsonIgnore`. If the API shape should differ from the entity,
  add DTOs and map them with `fun Entity.toResponse()` extensions in `response/ResponseExtensions.kt`.
- Validate request DTOs with `@Valid` and `@field:` constraints. Report business validation errors
  as `ValidationException(listOf(ApiValidation.Violation(fieldId = ..., message = ...)))`.
- List endpoints take `@ParameterObject pageable: Pageable` and `@Valid @ParameterObject filter`,
  and return `Pagination<<Resource>ListResponse>`, built in the service with `GridQueryBuilder`
  (see `TodoServiceImpl.list`).

## Persistence

- Public IDs: expose a UUID string `identifier`, never the numeric `id` (R2DBC) or the Mongo `_id`.
- R2DBC entities have `@Id id: Long?`, `@CreatedDate createdAt`, `@LastModifiedDate lastModifiedAt`
  (`Instant`), and `@Version version: Long?` for optimistic locking. Only user-owned entities have
  `createdBy: String` (the user's identity); shared entities don't.
- Let unique constraints in the database enforce uniqueness. Catch `DuplicateKeyException` and turn
  it into a `ValidationException`. Do not check first and then insert.
- Migrations: add a new `V<n+1>__...sql`. Never edit a migration that has already been applied.
  Name constraints `uk_<table>_<cols>` and indexes `idx_<table>_<cols>`. Use `DATETIME(6)` for
  timestamps.
- All time is UTC.

## Tests

- Unit tests (JUnit 5 + MockK, `runTest`) mirror the main package. Use the `spring-unit-tests` skill.
- Controller integration tests live in `integration/controller/` and extend
  `BaseControllerIntegrationTest` (`clientFor(user(...))`, `references`, `concurrently { }`).
  Use the `spring-controller-integration-tests` skill.
- Test names are backtick sentences: `` `list returns only the current user's todos` ``.
- Fixtures load only with the `test` and `shell` profiles. Document the fixture data each test
  class relies on in its KDoc.

## Enforcement

- `architecture/ConventionsTest.kt` extends `ServiceConventionsTest` from the kostack
  `service-conventions` library (Konsist, runs in `./gradlew test`) and enforces: controller
  naming and package, `suspend fun *Action` handlers, `version` on every mapping, the operationId
  pattern, no repositories in controllers, `XService`/`XServiceImpl` structure, coroutine
  repositories, no blocking or JDBC/JPA outside `configuration/`, and migration file names.
  If it fails, fix the code. Rules are changed in the library, for every service at once; the
  per-service options are `legacyOperationIds` and overriding a rule without `@Test`.
- Hooks in `.claude/settings.json`: committed migrations can't be edited, and Spotless runs
  automatically when the agent finishes.

## Skills in this repo

- `add-rest-resource`: scaffold a new resource end-to-end.
- `spring-unit-tests`, `spring-controller-integration-tests`, `code-review`.
