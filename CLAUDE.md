# Agent guide

Kotlin + Spring Boot (WebFlux, coroutines) service. Package root: `io.kostack.skeleton`.
README.MD has setup, profiles, and Docker details; this file holds the coding conventions.

## Build and verify

- `./gradlew test`: full suite. Integration tests use Testcontainers, so Docker must be running.
- `./gradlew check`: Spotless, then tests, then JaCoCo verification (minimum **80%** coverage;
  the shared plugin excludes `command`, `configuration` and `fixture`; this service also excludes
  `**/generated/**` and `**/request/**`).
- `./gradlew spotlessApply`: fix formatting. Run it before reporting a task done.
- Build logic comes from the kostack gradle-conventions plugins. Keep `build.gradle.kts` limited to
  what is specific to this service.

## Layout

```text
controller/<resource>/
  <Resource>Controller.kt        One REST controller per resource, with suspend fun <verb>Action handlers
  request/                       <Resource>CreateRequest, <Resource>UpdateRequest (validated HTTP bodies)
  response/                      <Resource>Response, <Resource>ListResponse, ResponseExtensions.kt
  filter/                        <Resource>ListFilter (query/sort definition for list endpoints)
  resolver/                      <Resource>ArgumentResolver (loads path entities; a @Component,
                                 registered automatically, never in WebFluxConfig)
  authorization/                 <Resource>Authorization (@Component("<resource>Auth"))
service/<resource>/              <Resource>Service + <Resource>ServiceImpl for business logic and persistence
db/r2dbc/{model,repository}      MySQL entities (@Table) and CoroutineCrudRepository
db/mongodb/{model,repository}    Mongo documents (@Document) and CoroutineCrudRepository
fixture/{r2dbc,mongodb}          AbstractFixture subclasses, ordered with @Order
resources/db/migration           Flyway: V<n>__<description>.sql
```

Reference implementations: `task` (shared; a parent reference, a user reference, PUT replacement,
a list scoped to a required parent resolved from a query parameter: `TaskController.listAction`)
and `project` (an owner set from the principal, an owner-scoped list, a `@PreAuthorize` owner rule,
an optional 1:1 reference, an aggregate count in the list, and a delete that removes its children
in one locked transaction: `ProjectServiceImpl.delete`). When in doubt, copy their shape.
No creator-private (`createdBy`) resource exists at the moment; follow the ownership rules below for one.

`controller/project/ProjectController` and `controller/task/TaskController` delegate to
`service/project/ProjectService` and `service/task/TaskService`. The current-user endpoint uses
`controller/me/MeController`, which maps the authenticated user directly to its response.

Controllers handle HTTP binding, request validation, authorization annotations, status codes and
HTTP exception mapping. They delegate business logic and persistence to services. Controllers do
not access repositories or `GridQueryBuilder` directly; resolvers and fixtures may use repositories.

- Put service contracts in `service/<resource>/<Resource>Service` and implementations in
  `<Resource>ServiceImpl` (`@Service`). Use suspend methods and coroutine repositories.
- Pass entities, identifiers and individual request fields to service methods. Keep request DTOs
  and HTTP status handling in controllers. Existing list methods accept `Pageable` and resource filters;
  services may return response DTOs using the extensions in `response/`.
- Services own business validation, reference lookup, list queries and transaction boundaries.
  Controllers translate optimistic locking failures into HTTP 409 responses.
- Stream consumers (`cloud/stream/`) and shell commands (`shell/`) reuse service methods when needed.
  Add only methods required by a caller. A controller that only maps the principal, such as
  `MeController`, needs no service.

## Kotlin style

- Follow `.editorconfig`: 2-space indent, 120-column lines, no wildcard imports, no trailing commas.
- Use data classes for models and DTOs. Use expression bodies when they fit.
- Everything is non-blocking. Use `suspend` functions and `CoroutineCrudRepository`. Never use
  `.block()`, `runBlocking`, JPA/JDBC repositories, or `Thread.sleep` (startup code in
  `configuration/` is the only exception). Where Spring requires a `Mono`
  (resolvers, `@PreAuthorize` beans), wrap the code with `kotlinx.coroutines.reactor.mono { }`.
- Don't add a custom Jackson `ObjectMapper` configuration. It was removed on purpose.

## API conventions

- Each resource has one `controller/<resource>/<Resource>Controller.kt`, annotated with
  `@RestController`, `@RequestMapping("/api/{version}/<plural>")` and `@Tag`.
  `MeController` maps `/api/{version}/me`.
- Handlers are `suspend fun <verb>Action(...)`, for example `listAction`, `getAction` and
  `createAction`. Add endpoints to the resource's controller.
- Every mapping declares `version = "1+"` (or a range when a version changes behavior).
- Add only the actions requested by name. A request to create a resource without named actions
  does not authorize adding CRUD handlers, DTOs, resolvers or tests for unspecified endpoints.
- Every endpoint has `@Operation(operationId = "<resource>_<verb>_action", summary = "...")`.
- Declare `produces`/`consumes = [MediaType.APPLICATION_JSON_VALUE]` explicitly.
- Status codes: create → `@ResponseStatus(HttpStatus.CREATED)`; update or delete without a body →
  `@ResponseStatus(HttpStatus.NO_CONTENT)`.
- Updates are `@PutMapping` replacements: every field comes from the request. Required fields are
  non-null in the request (missing → 400); an omitted or `null` optional field or reference clears it.
  Skip the save when nothing changed, and turn `OptimisticLockingFailureException` into
  `ResponseStatusException(HttpStatus.CONFLICT)`. Updating a reference changes only the entity being
  updated.
- Path entities: take `@PathVariable <resource>Id: String` plus
  `@Parameter(hidden = true) @ResolvedArgument entity: Entity`. The resolver returns 404 through
  `ResponseStatusException(HttpStatus.NOT_FOUND)`. Controllers never load their path entity themselves.
- Ownership comes in two forms; ask which one a new resource needs:
  - Creator-private (`createdBy`): the resolver loads by identifier and owner
    (`findByIdentifierAndCreatedBy(identifier, user.identity)`), so other users' entities are 404 and
    their existence isn't revealed.
  - Owner field with an owner rule (like `project`): `ownerId` is set from the principal and never
    accepted from the client; handlers restricted to the owner use
    `@PreAuthorize("@<resource>Auth.requireOwner(#entity, authentication.principal)")`, so other users
    get 403; the service scopes the list with `.where(x[Xxx::ownerId] eq ownerId)`,
    where `ownerId` comes from the authenticated principal.
  In a resolver, get the user with
  `exchange.getPrincipal<Authentication>().awaitSingleOrNull()?.principal as? User`, and throw
  `ResponseStatusException(HttpStatus.UNAUTHORIZED)` when it's missing.
  Never use `authentication.name`; it isn't the identity.
- Other access rules (roles, states) go on the handler as
  `@PreAuthorize("@<resource>Auth.<rule>(#entity, authentication.principal)")`.
- Get the current user in a handler with `@AuthenticationPrincipal user: User` (the Mongo `User`).
- Response DTOs are optional. Returning an entity directly is fine; hide internal fields with
  `@JsonIgnore`. If the API shape should differ from the entity,
  add DTOs and map them with `fun Entity.toResponse()` extensions in `response/ResponseExtensions.kt`.
  Related entities appear as small summary DTOs (`ProjectFeaturedTaskResponse`,
  `TaskAssigneeResponse`). A user summary holds only `id` (the identity) and `name`; email is exposed
  only through `GET /me`.
- Name HTTP body DTOs `<Resource><Action>Request` and put them in `controller/<resource>/request/`,
  for example `ProjectCreateRequest`, `ProjectUpdateRequest`, `TaskCreateRequest` and `TaskUpdateRequest`.
  Use the handler parameter name `request`. These DTOs describe HTTP input; service methods receive
  individual fields and resolved entities. Query filters stay in `filter/`.
- Validate request DTOs with `@Valid @RequestBody` and `@field:` constraints. Unknown fields are ignored,
  never checked or rejected (no `@JsonAnySetter` collectors, no `@Null` guards): fields a client may not
  set, such as an owner or an immutable
  parent id, are simply not part of the request. Report business validation errors as
  `ValidationException(listOf(ApiValidation.Violation(fieldId = ..., message = ...)))`.
- References in requests and responses are public ids: the related entity's `identifier`, or a
  user's `identity`. Load the related entity and report a missing or unsuitable one as a
  `ValidationException` on that field.
- List endpoints take `@ParameterObject pageable: Pageable` and `@Valid @ParameterObject filter`,
  and return `Pagination<<Resource>ListResponse>`, built in the service with `GridQueryBuilder`
  (see `TaskServiceImpl.list`). Use `.join(...)` with `@QueryField(table = ...)`/`@SelectColumn` for related
  columns, and `.aggregateJoin(...)` with `@SelectAggregate`/`@QueryField(aggregate = ...)` for counts
  (see `ProjectServiceImpl.list`).
- A list scoped to a required parent takes `@RequestParam parentId: String` plus
  `@ResolvedArgument parent: Parent`; the parent's resolver falls back from the path variable to the
  query parameter (see `ProjectArgumentResolver`). Unknown parent → 404, missing parameter → 400.
- Deleting an entity with children runs in one `TransactionalOperator.executeAndAwait { }`: lock the
  row (`findByIdForUpdate`), delete the children (`@Modifying @Query`), then the entity. Services
  that create children catch `DataIntegrityViolationException` and report a `ValidationException`
  on the parent id field (see `ProjectServiceImpl.delete`, `TaskServiceImpl.create`).

## Persistence

- Public IDs: expose a UUID string `identifier`, never the numeric `id` (R2DBC) or the Mongo `_id`.
- R2DBC entities have `@Id id: Long?`, `identifier: String = UUID.randomUUID().toString()`,
  `@CreatedDate createdAt`, `@LastModifiedDate lastModifiedAt` (`Instant`), and `@Version version: Long?`
  for optimistic locking. Only creator-private entities have `createdBy: String` (the user's
  identity).
- Entity enums live in `db/r2dbc/enums/` and are stored by name in a `VARCHAR` column.
- References store the internal `Long` id of another R2DBC entity (with a foreign key), or a user's
  `identity` string (MongoDB, no foreign key).
- Let unique constraints in the database enforce uniqueness. Catch `DuplicateKeyException` and turn
  it into a `ValidationException`. Do not check first and then insert.
- Migrations: add a new `V<n+1>__...sql`. Never edit a migration that has already been applied.
  Name constraints `uk_<table>_<cols>`, foreign keys `fk_<table>_<col>` and indexes
  `idx_<table>_<cols>`. Use `DATETIME(6)` for
  timestamps.
- All time is UTC. Use `java.time.Instant` for every date and time, including date-like fields such
  as due dates (column `DATETIME(6)`, JSON as an ISO-8601 instant). Never use `LocalDate`,
  `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime` or `java.util.Date`.

## Tests

- Controllers are tested through HTTP with one integration test class per controller:
  `integration/controller/<resource>/<Resource>ControllerIntegrationTest.kt`. They extend
  `BaseIntegrationTest` (`clientFor(user(...))`, `references`, `concurrently { }`) and cover the
  business rules and argument resolvers too. Use the `spring-controller-integration-tests` skill.
  Reference tests: `TaskControllerIntegrationTest` shows a `list(...)` helper for filter tests;
  `ProjectControllerIntegrationTest` shows race tests with `concurrently { }`.
- Unit tests (JUnit 5 + MockK, `runTest`) mirror the main package. Use the `spring-unit-tests` skill.
  In `controller/`, only authorization classes get unit tests; controllers and resolvers are covered
  by the integration tests. Outside `controller/`, unit test classes with their own logic, such as
  services and `security/`. Existing HTTP tests also exercise the project and task service behavior.
- Test names are backtick sentences: `` `list combines filters with AND` ``.
- Fixtures load only with the `test` and `shell` profiles. Document the fixture data each test
  class relies on in its KDoc. References can't be replaced (`setReference` throws): a fixture that
  updates an entity later saves it without re-setting the reference, and tests read such fields from
  the repository.

## Enforcement

- `architecture/ConventionsTest.kt` extends `ServiceConventionsTest` from the kostack
  `service-conventions` library, using its default `controller` package. It runs in `./gradlew test`
  and enforces: controllers live in `controller/` and are named `*Controller`; endpoint handlers
  are `suspend fun *Action`; every mapping declares `version`; every endpoint has an operationId
  matching the resource/verb/action pattern; controllers do not use repositories directly; service
  implementations are `@Service` classes implementing their interfaces; repositories are coroutine
  repositories; no blocking or JDBC/JPA outside `configuration/`; and valid migration file names.
  If it fails, fix the code. Rules are changed in the library, for every service at once; the
  per-service options are `legacyOperationIds` and overriding a rule without `@Test`.
- Hooks in `.claude/settings.json`: committed migrations can't be edited, and Spotless runs
  automatically when the agent finishes.

## Skills in this repo

- `add-rest-resource`: scaffold a new resource end-to-end. Follow this guide for controller/service
  layout, `*Request` DTO naming and controller integration tests when a skill describes older conventions.
- `spring-unit-tests`, `spring-controller-integration-tests`, `code-review`.
