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
api/<resource>/
  operation/                    <Resource>Operation (composed annotation) + one class per endpoint:
                                List<Resource>, Get<Resource>, Create<Resource>, Complete<Resource>, ...
  command/                      <Resource>CreateCommand, ... (request bodies with jakarta validation)
  response/                     <Resource>Response, <Resource>ListResponse, ResponseExtensions.kt
  filter/                       <Resource>ListFilter (query/sort definition for list endpoints)
  resolver/                     <Resource>ArgumentResolver (loads path entities; a @Component,
                                registered automatically, never in WebFluxConfig)
  authorization/                <Resource>Authorization (@Component("<resource>Auth"))
service/<resource>/             <Resource>Service + <Resource>ServiceImpl, only when needed (see below)
db/r2dbc/{model,repository}     MySQL entities (@Table) and CoroutineCrudRepository
db/mongodb/{model,repository}   Mongo documents (@Document) and CoroutineCrudRepository
fixture/{r2dbc,mongodb}         AbstractFixture subclasses, ordered with @Order
resources/db/migration          Flyway: V<n>__<description>.sql
```

Reference implementation for everything: the `task` resource (shared, not user-owned). When in doubt,
copy its shape. `project` adds an owner field and a `@PreAuthorize` owner rule (`ProjectAuthorization`).
No user-owned (`createdBy`) resource exists at the moment; follow the ownership rules below for one.

By default there is no service layer. Each operation class is the use case: it handles one endpoint
and reads and writes through repositories (and `GridQueryBuilder` for lists) directly. Resolvers and
fixtures use repositories too.

Add a service only when the logic is needed outside an API operation, for example from a stream
consumer (`cloud/stream/`) or a shell command (`shell/`):
- Move the logic into `service/<resource>/<Resource>Service` (interface) and
  `<Resource>ServiceImpl` (`@Service`). Its methods take plain parameters, not request commands or
  HTTP types.
- The operation then delegates to the service too, so the logic exists in one place. It keeps only
  HTTP concerns (binding, status codes, the response mapping).
- Add only the methods the non-API caller needs. Don't create services in advance or "for
  consistency".
- Services are outside `api/`, so they get unit tests.

## Kotlin style

- Follow `.editorconfig`: 2-space indent, 120-column lines, no wildcard imports, no trailing commas.
- Use data classes for models and DTOs. Use expression bodies when they fit.
- Everything is non-blocking. Use `suspend` functions and `CoroutineCrudRepository`. Never use
  `.block()`, `runBlocking`, JPA/JDBC repositories, or `Thread.sleep` (startup code in
  `configuration/` is the only exception). Where Spring requires a `Mono`
  (resolvers, `@PreAuthorize` beans), wrap the code with `kotlinx.coroutines.reactor.mono { }`.
- Don't add a custom Jackson `ObjectMapper` configuration. It was removed on purpose.

## API conventions

- Each resource has a composed annotation `api/<resource>/operation/<Resource>Operation.kt`
  carrying `@RestController`, `@RequestMapping("/api/{version}/<plural>")` and `@Tag`. Every
  operation class in that package is annotated with it and has exactly one handler:
  `suspend operator fun invoke(...)` with the mapping annotation. Exception: `MeOperation` maps
  `/api/{version}`, so the current-user endpoint stays at `/api/{version}/me`.
- Every mapping declares `version = "1+"` (or a range when a version changes behavior).
- The conventions still accept classic `<Resource>Controller` classes with `suspend fun <verb>Action`
  handlers, but don't write new ones: every resource uses operation classes.
- Add only the actions that were requested by name, as operation classes. "Create the X resource" with
  no actions means only the `<Resource>Operation` annotation. Never add CRUD operations (or their DTOs,
  resolvers and tests) that nobody asked for.
- Every endpoint has `@Operation(operationId = "<resource>_<verb>_action", summary = "...")`.
- Declare `produces`/`consumes = [MediaType.APPLICATION_JSON_VALUE]` explicitly.
- Status codes: create → `@ResponseStatus(HttpStatus.CREATED)`; update or delete without a body →
  `@ResponseStatus(HttpStatus.NO_CONTENT)`.
- Path entities: take `@PathVariable <resource>Id: String` plus
  `@Parameter(hidden = true) @ResolvedArgument entity: Entity`. The resolver returns 404 through
  `ResponseStatusException(HttpStatus.NOT_FOUND)`. Operations never load their path entity themselves.
- Ownership: for user-owned resources the resolver loads by identifier and owner
  (`findByIdentifierAndCreatedBy(identifier, user.identity)`), so other users' entities are 404 and
  their existence isn't revealed. In the resolver, get the user with
  `exchange.getPrincipal<Authentication>().awaitSingleOrNull()?.principal as? User`, and throw
  `ResponseStatusException(HttpStatus.UNAUTHORIZED)` when it's missing.
  Never use `authentication.name`; it isn't the identity.
- Further access rules (roles, states) go on the handler as
  `@PreAuthorize("@<resource>Auth.<rule>(#entity, authentication.principal)")`.
- Get the current user in a handler with `@AuthenticationPrincipal user: User` (the Mongo `User`).
- Response DTOs are optional. Returning an entity directly is fine; hide internal fields with
  `@JsonIgnore`. If the API shape should differ from the entity,
  add DTOs and map them with `fun Entity.toResponse()` extensions in `response/ResponseExtensions.kt`.
- Request bodies are commands (`command/<Resource>CreateCommand`), validated with `@Valid` and
  `@field:` constraints. Report business validation errors as
  `ValidationException(listOf(ApiValidation.Violation(fieldId = ..., message = ...)))`.
- List endpoints take `@ParameterObject pageable: Pageable` and `@Valid @ParameterObject filter`,
  and return `Pagination<<Resource>ListResponse>`, built in the operation with `GridQueryBuilder`
  (see `ListTask`).

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

- Operations are tested through HTTP only, with one integration test class per operation:
  `integration/api/<resource>/operation/<Operation>IntegrationTest.kt` (for example
  `CreateTaskIntegrationTest`). They extend `BaseIntegrationTest` (`clientFor(user(...))`,
  `references`, `concurrently { }`) and cover the operation's business rules too. Use the
  `spring-controller-integration-tests` skill.
- Unit tests (JUnit 5 + MockK, `runTest`) mirror the main package. Use the `spring-unit-tests` skill.
  In `api/`, only authorization classes get unit tests; operations and resolvers are covered by
  the integration tests. Outside `api/`, unit test classes with their own logic (`security/`).
- Test names are backtick sentences: `` `list combines filters with AND` ``.
- Fixtures load only with the `test` and `shell` profiles. Document the fixture data each test
  class relies on in its KDoc.

## Enforcement

- `architecture/ConventionsTest.kt` extends `ServiceConventionsTest` (with `apiPackage = "api"`) from the kostack
  `service-conventions` library (Konsist, runs in `./gradlew test`) and enforces: controllers
  (including classes with a composed `@RestController` annotation) live in `api/`; operation
  classes in `api..operation` have a `suspend operator fun invoke` handler, other controllers
  are `*Controller` with `suspend fun *Action`; `version` on every mapping; the operationId pattern;
  no repositories in classic controllers (operations may use them); coroutine repositories; no
  blocking or JDBC/JPA outside `configuration/`; and migration file names.
  If it fails, fix the code. Rules are changed in the library, for every service at once; the
  per-service options are `legacyOperationIds` and overriding a rule without `@Test`.
- Hooks in `.claude/settings.json`: committed migrations can't be edited, and Spotless runs
  automatically when the agent finishes.

## Skills in this repo

- `add-rest-resource`: scaffold a new resource end-to-end.
- `spring-unit-tests`, `spring-controller-integration-tests`, `code-review`.
