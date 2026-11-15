---
name: add-rest-resource
description: Scaffold a new REST resource (entity, repository, service, controller, DTOs, authorization, fixture, migration, tests) in this Kotlin/Spring WebFlux service following the todo reference implementation. Use when asked to add a new entity, domain object, endpoint set, controller, CRUD API, or resource.
---

# Add a REST resource

The `todo` resource is the reference implementation. Before writing anything, read these files and
mirror their structure, annotations and naming exactly:

- `src/main/kotlin/io/kostack/skeleton/controller/todo/` (all subpackages)
- `src/main/kotlin/io/kostack/skeleton/service/todo/`
- `src/main/kotlin/io/kostack/skeleton/db/r2dbc/` (or `db/mongodb/` for documents)
- `src/main/kotlin/io/kostack/skeleton/fixture/r2dbc/TodoFixture.kt`
- `src/main/resources/db/migration/` (latest migration)
- `src/test/kotlin/io/kostack/skeleton/integration/controller/TodoControllerTest.kt`

Follow the conventions in `CLAUDE.md`. Below, `Xxx` is the singular PascalCase name, `xxx` the
camelCase name, and `xxxs` the plural used in URLs and table names.

## 1. Clarify

Ask only if it isn't clear from the request:
- Which fields, and which of them are required or unique?
- Storage: **r2dbc/MySQL** (default for relational, user-owned data) or **MongoDB**?
- Which actions: list, get, create, update, delete, or custom state transitions?
- Ownership: is it owned by the creating user (`createdBy` plus `requireOwner`), or shared?

## 2. Persistence

R2DBC:
1. `db/r2dbc/model/Xxx.kt`: a `@Table("xxxs")` data class with `@Id id: Long? = null`,
   `identifier: String`, the domain fields, `@CreatedDate createdAt`, `@LastModifiedDate lastModifiedAt`,
   `createdBy: String`, and `@Version version: Long? = null`.
2. `db/r2dbc/repository/XxxRepository.kt`: `CoroutineCrudRepository<Xxx, Long>` with
   `suspend fun findByIdentifier(identifier: String): Xxx?`.
3. A new `src/main/resources/db/migration/V<next>__create_xxxs.sql` matching the entity: snake_case
   columns, `DATETIME(6)` timestamps, `version BIGINT NOT NULL DEFAULT 0`,
   `CONSTRAINT uk_xxxs_identifier UNIQUE (identifier)`, and indexes named `idx_xxxs_<cols>` for every
   filter and sort column. Never modify an existing migration.

MongoDB: a `@Document("xxxs")` class in `db/mongodb/model` with `@Id id: String? = null`, unique fields
`@Indexed(unique = true)`, and `@Version`. The repository is `CoroutineCrudRepository<Xxx, String>`.

## 3. Service

- `service/xxx/XxxService.kt` (interface) and `XxxServiceImpl.kt` (`@Service`). All methods are
  `suspend`.
- Include `findByIdentifier` (the argument resolver needs it) and `save`.
- `create(request, user)` generates `identifier = UUID.randomUUID().toString()` and sets
  `createdBy = user.identity`.
- Let unique indexes enforce uniqueness: catch `DuplicateKeyException` and throw
  `ValidationException(listOf(ApiValidation.Violation(fieldId = "...", message = "...")))`.
- State transitions must be idempotent where it makes sense. Swallow
  `OptimisticLockingFailureException` only when a concurrent writer reaching the same state is
  harmless (see `TodoServiceImpl.complete`).
- `list(pageable, filter, user)` uses `GridQueryBuilder` + `table<Xxx>("X")` and
  `pageable.toPageRequest(XxxListFilter::class)`, scoped to the owner.

## 4. Controller package `controller/xxx/`

- `request/XxxCreateRequest.kt` (and `XxxUpdateRequest` if needed): constraints use `@field:`.
- `response/XxxResponse.kt`, `response/XxxListResponse.kt` (lean list projection), and
  `response/ResponseExtensions.kt` with `fun Xxx.toResponse()`. Expose `identifier`, never `id`.
- `filter/XxxListFilter.kt`: `@SortableDefaults`, one `@Sortable` per sortable field, and
  `@QueryField` plus `@field:Schema(required = false)` on optional filter fields.
- `resolver/XxxArgumentResolver.kt`: reads the `xxxId` path parameter and throws
  `ResponseStatusException(HttpStatus.NOT_FOUND)` when the entity is missing.
- `authorization/XxxAuthorization.kt`: `@Component("xxxAuth")` with
  `fun requireOwner(xxx: Xxx, principal: User): Mono<Boolean> = mono { ... }`.
- `XxxController.kt`:
  - `@Tag`, `@RequestMapping("/api/{version}/xxxs")`, `@RestController`
  - methods `suspend fun listAction / getAction / createAction / updateAction / deleteAction`
  - `@Operation(operationId = "xxx_<verb>_action", summary = "...")` on each
  - `version = "1+"` and explicit `produces`/`consumes` on each mapping
  - `@ResponseStatus(HttpStatus.CREATED)` for create, `NO_CONTENT` for body-less updates and deletes
  - path endpoints take `@PathVariable xxxId: String` +
    `@Parameter(hidden = true) @ResolvedArgument xxx: Xxx` and
    `@PreAuthorize("@xxxAuth.requireOwner(#xxx, authentication.principal)")`

## 5. Fixtures and manual calls

- `fixture/r2dbc/XxxFixture.kt`: extends `AbstractFixture`, gets an `@Order` after the fixtures it
  references, and stores references as `xxx_<n>`. Cover data for at least two users, so tests can
  check ownership.
- `http/xxx.http`: one request per endpoint, using `{{baseUrl}}` and `{{authToken}}` (see `http/me.http`).

## 6. Tests

- Unit tests for `XxxServiceImpl` and `XxxAuthorization`: follow the `spring-unit-tests` skill.
- `integration/controller/XxxControllerTest.kt`: follow the `spring-controller-integration-tests`
  skill. Document the fixture data in the class KDoc.

## 7. Verify

1. `./gradlew spotlessApply`
2. `./gradlew check`: formatting, tests, and the 80% coverage gate must all pass.
3. Report the files you created, the endpoints with methods and status codes, and anything you
   left out or assumed.

## Checklist

- [ ] No entity is returned from a controller; there is a `toResponse()` for each response DTO
- [ ] Every mapping has `version = "1+"` and an `operationId` of the form `xxx_<verb>_action`
- [ ] Every path endpoint has `@ResolvedArgument` + `@PreAuthorize`
- [ ] Uniqueness is enforced by the database and mapped to `ValidationException`
- [ ] A new migration exists, and no existing migration was edited
- [ ] No blocking calls (`.block()`, JDBC, `Thread.sleep`)
- [ ] Fixture, `.http` file, unit tests, and integration tests are added
