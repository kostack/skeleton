---
name: add-rest-resource
description: Scaffold a new REST resource (entity, repository, operation classes, commands, DTOs, resolver, authorization, fixture, migration, tests) in this Kotlin/Spring WebFlux service following the todo reference implementation. Use when asked to add a new entity, domain object, endpoint set, controller, operation, CRUD API, or resource.
---

# Add a REST resource

The `todo` resource is the reference implementation (user-owned). `project` is the reference for a
shared resource. Before writing anything, read these files and mirror their structure, annotations
and naming exactly:

- `src/main/kotlin/io/kostack/skeleton/api/todo/` (all subpackages, especially `operation/`)
- `src/main/kotlin/io/kostack/skeleton/db/r2dbc/` (or `db/mongodb/` for documents)
- `src/main/kotlin/io/kostack/skeleton/fixture/r2dbc/TodoFixture.kt`
- `src/main/resources/db/migration/` (latest migration)
- `src/test/kotlin/io/kostack/skeleton/integration/api/todo/operation/` (integration tests)

Follow the conventions in `CLAUDE.md`. Below, `Xxx` is the singular PascalCase name, `xxx` the
camelCase name, and `xxxs` the plural used in URLs and table names.

Operation classes, resolvers and fixtures use repositories directly. Don't create `service/` classes
when scaffolding a resource: a service is added later, only when logic is needed outside an API
operation (see `CLAUDE.md`).

## 1. Clarify

Ask only if it isn't clear from the request:
- Which fields, and which of them are required or unique?
- Storage: **r2dbc/MySQL** (default for relational, user-owned data) or **MongoDB**?
- Which actions: list, get, create, update, delete, or custom state transitions? Build only the
  actions that were requested by name. Never add the rest of the CRUD set "for completeness". If the
  request doesn't name any actions, create no operation classes: build only what was asked for, such
  as the entity and the `XxxOperation` annotation.
- Ownership: is it owned by the creating user (`createdBy`, resolver scoped to the owner), or shared?
  Shared resources (catalogs, projects that everyone can see) have no `createdBy`. If it's unclear,
  ask instead of adding the field by default.

## 2. Persistence

R2DBC:
1. `db/r2dbc/model/Xxx.kt`: a `@Table("xxxs")` data class with `@Id id: Long? = null`,
   `identifier: String`, the domain fields, `@CreatedDate createdAt`, `@LastModifiedDate lastModifiedAt`,
   and `@Version version: Long? = null`. Add `createdBy: String` (the user's identity, column
   `created_by VARCHAR(36) NOT NULL`) only for user-owned resources.
2. `db/r2dbc/repository/XxxRepository.kt`: `CoroutineCrudRepository<Xxx, Long>` with only the query
   methods the requested actions need. When an action takes the entity from the path, add
   `suspend fun findByIdentifier(identifier: String): Xxx?` for shared resources, or
   `suspend fun findByIdentifierAndCreatedBy(identifier: String, createdBy: String): Xxx?` for
   user-owned ones.
3. A new `src/main/resources/db/migration/V<next>__create_xxxs.sql` matching the entity: snake_case
   columns, `DATETIME(6)` timestamps, `version BIGINT NOT NULL DEFAULT 0`,
   `CONSTRAINT uk_xxxs_identifier UNIQUE (identifier)`, and indexes named `idx_xxxs_<cols>` for every
   filter and sort column. Never modify an existing migration.

MongoDB: a `@Document("xxxs")` class in `db/mongodb/model` with `@Id id: String? = null`, unique fields
`@Indexed(unique = true)`, and `@Version`. The repository is `CoroutineCrudRepository<Xxx, String>`.

## 3. Package `api/xxx/`

Add each of these only when a requested action uses it.

- `operation/XxxOperation.kt`: the composed annotation for the resource (copy `TodoOperation`):
  ```kotlin
  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.RUNTIME)
  @MustBeDocumented
  @RestController
  @RequestMapping("/api/{version}/xxxs")
  @Tag(name = "Xxx", description = "Api operations for managing xxxs")
  annotation class XxxOperation
  ```
- `command/XxxCreateCommand.kt` (and `XxxUpdateCommand` if needed): request bodies; constraints use
  `@field:`.
- Responses (optional): returning the entity directly is fine. Hide internal fields such as `id` and
  `version` with `@JsonIgnore`. If the API shape should differ from the entity, add
  `response/XxxResponse.kt` and `response/ResponseExtensions.kt` with `fun Xxx.toResponse()`.
  For a list endpoint that uses `GridQueryBuilder`, add a lean `response/XxxListResponse.kt` projection.
- `filter/XxxListFilter.kt`: `@SortableDefaults`, one `@Sortable` per sortable field, and
  `@QueryField` plus `@field:Schema(required = false)` on optional filter fields.
- `resolver/XxxArgumentResolver.kt`: a `@Component` (Spring registers it automatically; never add it to
  `WebFluxConfig`) that reads the `xxxId` path parameter, loads through the repository
  and throws `ResponseStatusException(HttpStatus.NOT_FOUND)` when the entity is missing. For
  user-owned resources, load by identifier and owner, taking the user from
  `exchange.getPrincipal<Authentication>().awaitSingleOrNull()?.principal as? User` and throwing
  `ResponseStatusException(HttpStatus.UNAUTHORIZED)` when it's missing (never `authentication.name`).
  See `TodoArgumentResolver`.
- `authorization/XxxAuthorization.kt` (only for access rules beyond ownership, such as roles or
  states): `@Component("xxxAuth")` with `fun <rule>(xxx: Xxx, principal: User): Mono<Boolean> = mono { ... }`.

## 4. Operation classes `api/xxx/operation/`

One class per requested action, named `<Verb>Xxx` (`ListXxx`, `GetXxx`, `CreateXxx`, `UpdateXxx`,
`DeleteXxx`, or a custom verb such as `CompleteTodo`), and no others:

- annotated with `@XxxOperation` only
- constructor-inject the repositories (and `GridQueryBuilder` for lists) it needs
- exactly one handler: `suspend operator fun invoke(...)` with the mapping annotation
- `@Operation(operationId = "xxx_<verb>_action", summary = "...")` on the handler
- `version = "1+"` and explicit `produces`/`consumes` on the mapping
- `@ResponseStatus(HttpStatus.CREATED)` for create, `NO_CONTENT` for body-less updates and deletes
- path endpoints take `@PathVariable xxxId: String` +
  `@Parameter(hidden = true) @ResolvedArgument xxx: Xxx`
- add `@PreAuthorize("@xxxAuth.<rule>(#xxx, authentication.principal)")` only for access rules beyond
  ownership

Logic inside the handler:
- Create generates `identifier = UUID.randomUUID().toString()` and, for user-owned resources, sets
  `createdBy = user.identity` (take `@AuthenticationPrincipal user: User`). Shared resources don't need
  the user.
- Let unique indexes enforce uniqueness: catch `DuplicateKeyException` around `repository.save` and
  throw `ValidationException(listOf(ApiValidation.Violation(fieldId = "...", message = "...")))`.
  Never check first and then insert.
- State transitions must be idempotent where it makes sense. Swallow
  `OptimisticLockingFailureException` only when a concurrent writer reaching the same state is
  harmless (see `CompleteTodo`).
- List uses `GridQueryBuilder` + `table<Xxx>("x")` and `pageable.toPageRequest(XxxListFilter::class)`
  (see `ListTodo`). Scope it to the owner (`createdBy`) only for user-owned resources.

## 5. Fixtures

- `fixture/r2dbc/XxxFixture.kt`: extends `AbstractFixture`, injects the repository, gets an `@Order`
  after the fixtures it references, and stores references as `xxx_<n>`. For user-owned resources,
  cover data for at least two users, so tests can check ownership.

## 6. Tests

- Operations are tested through HTTP only: one
  `integration/api/xxx/operation/<Verb>XxxIntegrationTest.kt` per operation, extending
  `BaseIntegrationTest`. Follow the `spring-controller-integration-tests` skill. Document the fixture
  data in the class KDoc. Cover the operation's business rules there (validation, duplicates,
  idempotent transitions, concurrency where it matters). For user-owned resources, test that another
  user's entity returns 404.
- Unit tests only for `XxxAuthorization`, if it exists: follow the `spring-unit-tests` skill. Don't
  write unit tests for operations, resolvers or other classes in `api/xxx/`; the integration
  tests cover them (the resolver through its 404 cases).

## 7. Verify

1. `./gradlew spotlessApply`
2. `./gradlew check`: formatting, tests (including `ConventionsTest`), and the 80% coverage gate must
   all pass.
3. Report the files you created, the endpoints with methods and status codes, and anything you
   left out or assumed.

## Checklist

- [ ] `XxxOperation` exists, and every operation class uses it with one `suspend operator fun invoke`
- [ ] Entities returned directly don't expose internal fields; any response DTOs have a `toResponse()`
- [ ] Every mapping has `version = "1+"` and an `operationId` of the form `xxx_<verb>_action`
- [ ] Path endpoints load their entity with `@ResolvedArgument`; user-owned ones resolve by owner (404)
- [ ] Uniqueness is enforced by the database and mapped to `ValidationException`
- [ ] A new migration exists, and no existing migration was edited
- [ ] No `service/` classes in the scaffold; no blocking calls (`.block()`, JDBC, `Thread.sleep`)
- [ ] Only the requested actions exist, with no extra operations, repository methods or DTOs
- [ ] Fixture and one integration test class per operation are added (unit tests only for authorization rules)
