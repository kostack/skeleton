---
name: add-rest-resource
description: Scaffold a new REST resource (entity, repository, operation classes, commands, DTOs, resolver, authorization, fixture, migration, tests) in a Kotlin/Spring WebFlux service, following the reference implementation named in the project's CLAUDE.md. Use when asked to add a new entity, domain object, endpoint set, controller, operation, CRUD API, or resource.
---

# Add a REST resource

The project's `CLAUDE.md` names the reference implementations, the base package and the
conventions. Before writing anything, read it, then read the reference resource closest to the
request and mirror its structure, annotations and naming exactly:

- its `api/<resource>/` package (all subpackages, especially `operation/`)
- the persistence packages (`db/r2dbc/` or `db/mongodb/`: models, enums, repositories)
- its fixture
- the latest migration in `src/main/resources/db/migration/`
- its integration tests

Below, `<base>` is the base package path from `CLAUDE.md` (for example `src/main/kotlin/<base>/`),
`Xxx` is the singular PascalCase name, `xxx` the camelCase name, and `xxxs` the plural used in URLs
and table names. Where this skill and `CLAUDE.md` disagree, `CLAUDE.md` wins.

Operation classes, resolvers and fixtures use repositories directly. Don't create `service/` classes
when scaffolding a resource: a service is added later, only when logic is needed outside an API
operation.

## 1. Clarify

Ask only if it isn't clear from the request:
- Which fields, and which of them are required or unique?
- Storage: **r2dbc/MySQL** (default for relational data) or **MongoDB**?
- Which actions: list, get, create, update, delete, or custom state transitions? Build only the
  actions that were requested by name. Never add the rest of the CRUD set "for completeness". If the
  request doesn't name any actions, create no operation classes: build only what was asked for, such
  as the entity and the `XxxOperation` annotation.
- Ownership and visibility: who may read, list and change it?
  - shared: every authenticated user can see and change it;
  - owner field with owner rules: the owner is set from the principal, owner-only handlers are
    guarded with `@PreAuthorize` (403 for others), and lists may be scoped to the owner;
  - creator-private (`createdBy`): the resolver loads by identifier and owner (404 for others).
  If it's unclear, ask instead of picking one.
- Relationships: which other entities it references, whether each reference is required, and what
  the cardinality is (1:N, N:1, optional 1:1).

## 2. Persistence

R2DBC:
1. `db/r2dbc/model/Xxx.kt`: a `@Table("xxxs")` data class with `@Id id: Long? = null`,
   `identifier: String = UUID.randomUUID().toString()`, the domain fields,
   `@CreatedDate createdAt`, `@LastModifiedDate lastModifiedAt` (both `Instant = Instant.now()`), and
   `@Version version: Long? = null`. Add `createdBy: String` (column `created_by VARCHAR(36) NOT NULL`)
   only for creator-private resources.
2. Enums used by entities go in `db/r2dbc/enums/`. They are stored by name in a `VARCHAR` column;
   give the field a default when the domain has one.
3. References:
   - to another R2DBC entity: its internal `Long` id (`parentId: Long`, `optionalId: Long?`), with a
     foreign key in the migration;
   - to a user stored elsewhere (for example MongoDB): the user's identity string (`VARCHAR(36)`),
     with no foreign key.
4. `db/r2dbc/repository/XxxRepository.kt`: `CoroutineCrudRepository<Xxx, Long>` with only the query
   methods the requested actions need. When an action takes the entity from the path, add
   `suspend fun findByIdentifier(identifier: String): Xxx?` (or
   `findByIdentifierAndCreatedBy(identifier, createdBy)` for creator-private resources).
5. A new `src/main/resources/db/migration/V<next>__<description>.sql` matching the entity: snake_case
   columns, `DATETIME(6)` timestamps, `version BIGINT NOT NULL DEFAULT 0`,
   `CONSTRAINT uk_xxxs_identifier UNIQUE (identifier)`, foreign keys named `fk_xxxs_<col>`, and
   indexes named `idx_xxxs_<cols>` for every filter and sort column. An optional 1:1 reference also
   gets `uk_xxxs_<col>`. For circular references, create both tables first and add the second
   foreign key with `ALTER TABLE`. Never modify an existing migration.

MongoDB: a `@Document("xxxs")` class in `db/mongodb/model` with `@Id id: String? = null`, unique fields
`@Indexed(unique = true)`, and `@Version`. The repository is `CoroutineCrudRepository<Xxx, String>`.

## 3. Package `api/xxx/`

Add each of these only when a requested action uses it.

- `operation/XxxOperation.kt`: the composed annotation for the resource:
  ```kotlin
  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.RUNTIME)
  @MustBeDocumented
  @RestController
  @RequestMapping("/api/{version}/xxxs")
  @Tag(name = "Xxx", description = "Api operations for managing xxxs")
  annotation class XxxOperation
  ```
- `command/XxxCreateCommand.kt` and `command/XxxUpdateCommand.kt`: request bodies; constraints use
  `@field:`. References are public ids: the related entity's `identifier`, or a user's identity.
  Never accept internal ids or fields the server sets (owner, `createdBy`); they come from the
  principal.
- Responses: returning the entity directly is fine if internal fields are hidden with `@JsonIgnore`.
  As soon as the shape differs (references as public ids, nested summaries), add
  `response/XxxResponse.kt` and `response/ResponseExtensions.kt` with `fun Xxx.toResponse(...)`,
  taking the loaded related entities as parameters. Summaries of related entities are small DTOs in
  the same package. A user summary has only `id` (the identity) and `name`; keep personal data such
  as email out of it.
- `response/XxxListResponse.kt` for a `GridQueryBuilder` list: a lean projection. Read columns of a
  joined table with `@SelectColumn(table = "<alias>", name = "<property>")`, and aggregates over an
  `aggregateJoin` table with `@SelectAggregate(table = "<alias>", function = Aggregate.COUNT)`.
- `filter/XxxListFilter.kt`: `@SortableDefaults`, one `@Sortable` per sortable field (DTO properties,
  including aggregates, or root entity properties), and `@QueryField` plus
  `@field:Schema(required = false)` on optional filter fields. A required filter is nullable with
  `@field:NotBlank` and `@field:Schema(required = true)`, so a missing value is a 400. Filter on a
  joined table with `@QueryField(table = "<alias>", name = "<property>")`, and on an aggregate with
  `@QueryField(table = "<alias>", aggregate = Aggregate.COUNT, operator = QueryOperator.GTE)` plus
  `@Min(0)`.
- `resolver/XxxArgumentResolver.kt`: a `@Component` (Spring registers it automatically; never add it to
  the WebFlux configuration) that reads the `xxxId` path parameter, loads through the repository
  and throws `ResponseStatusException(HttpStatus.NOT_FOUND)` when the entity is missing. For
  creator-private resources, load by identifier and owner, taking the user from
  `exchange.getPrincipal<Authentication>().awaitSingleOrNull()?.principal as? User` and throwing
  `ResponseStatusException(HttpStatus.UNAUTHORIZED)` when it's missing (never `authentication.name`).
- `authorization/XxxAuthorization.kt` (for access rules such as owner-only, roles or states):
  `@Component("xxxAuth")` with `fun <rule>(xxx: Xxx, principal: User): Mono<Boolean> = mono { ... }`.

## 4. Operation classes `api/xxx/operation/`

One class per requested action, named `<Verb>Xxx` (`ListXxx`, `GetXxx`, `CreateXxx`, `UpdateXxx`,
`DeleteXxx`, or a custom verb such as `CompleteXxx`), and no others:

- annotated with `@XxxOperation` only
- constructor-inject the repositories (and `GridQueryBuilder` for lists) it needs
- exactly one handler: `suspend operator fun invoke(...)` with the mapping annotation
- `@Operation(operationId = "xxx_<verb>_action", summary = "...")` on the handler
- `version = "1+"` and explicit `produces`/`consumes` on the mapping
- `@ResponseStatus(HttpStatus.CREATED)` for create, `NO_CONTENT` for body-less updates and deletes
- path endpoints take `@PathVariable xxxId: String` +
  `@Parameter(hidden = true) @ResolvedArgument xxx: Xxx`
- add `@PreAuthorize("@xxxAuth.<rule>(#xxx, authentication.principal)")` for access rules

Logic inside the handler:
- Create relies on the entity's `identifier` default. Set owner fields from the principal
  (`@AuthenticationPrincipal user: User`).
- Related ids from a command: load the related entity by its public id and throw
  `ValidationException(listOf(ApiValidation.Violation(fieldId = "<field>", message = "...")))` when it
  doesn't exist or doesn't qualify (for example it belongs to another parent). Store the internal id
  (or the user's identity).
- Update is a `@PutMapping` that replaces the resource: every field comes from the command. Required
  fields are non-null in the command (a missing one is a 400); an omitted or `null` optional field or
  reference clears it. Skip `save` when nothing changed, and turn `OptimisticLockingFailureException`
  into `ResponseStatusException(HttpStatus.CONFLICT)`. Updating a reference changes only this entity,
  never the referenced one.
- Let unique indexes enforce uniqueness: catch `DuplicateKeyException` around `repository.save` and
  throw a `ValidationException` for the field. Never check first and then insert.
- State transitions must be idempotent where it makes sense. Swallow
  `OptimisticLockingFailureException` only when a concurrent writer reaching the same state is
  harmless (for example an idempotent `CompleteXxx` transition).
- List uses `GridQueryBuilder` + `table<Xxx>("x")` and `pageable.toPageRequest(XxxListFilter::class)`.
  Use `.join(parent).on(...)` when a filter or column comes from a related table,
  `.aggregateJoin(child).on(...)` for counts, and `.where(x[Xxx::ownerId] eq principal.identity)` to
  scope the list to the current user.
- Get loads the related entities it returns as summaries and maps them with `toResponse(...)`.

## 5. Fixtures

- `fixture/r2dbc/XxxFixture.kt`: extends `AbstractFixture`, injects the repository, gets an `@Order`
  after the fixtures it references, and stores references as `xxx_<n>`. Cover data for at least two
  users when access depends on the user, and every value the filters and rules need (each enum
  value, set and unset optional references, a parent without children).
- References can't be replaced: `setReference` throws when the name exists. When a fixture updates an
  entity another fixture created, save it without setting the reference again; tests then read such
  fields from the repository.

## 6. Tests

- Operations are tested through HTTP only: one `<Verb>XxxIntegrationTest` per operation, in the
  integration test package and with the base class named in `CLAUDE.md`. Follow the
  `spring-controller-integration-tests` skill. Document the fixture data in the class KDoc. Cover the
  operation's business rules there (validation, unknown related ids, duplicates, PUT clearing optional
  fields, access rules, idempotent transitions, concurrency where it matters).
- Unit tests only for `XxxAuthorization`, if it exists: follow the `spring-unit-tests` skill. Don't
  write unit tests for operations, resolvers or other classes in `api/xxx/`; the integration
  tests cover them (the resolver through its 404 cases).

## 7. Verify

1. Run the formatter and the full check task named in `CLAUDE.md` (for example
   `./gradlew spotlessApply` and `./gradlew check`). Formatting, tests (including convention tests)
   and the coverage gate must all pass.
2. Report the files you created, the endpoints with methods and status codes, and anything you
   left out or assumed.

## Checklist

- [ ] `XxxOperation` exists, and every operation class uses it with one `suspend operator fun invoke`
- [ ] Entities returned directly don't expose internal fields; any response DTOs have a `toResponse()`
- [ ] Every mapping has `version = "1+"` and an `operationId` of the form `xxx_<verb>_action`
- [ ] Path endpoints load their entity with `@ResolvedArgument`; access rules use `@PreAuthorize` or an
      owner-scoped resolver
- [ ] References are public ids in the API, validated as `ValidationException` on their field
- [ ] Updates are PUT replacements: required fields non-null, omitted or `null` optionals clear
- [ ] Uniqueness is enforced by the database and mapped to `ValidationException`
- [ ] A new migration exists with `uk_`/`fk_`/`idx_` names, and no existing migration was edited
- [ ] No `service/` classes in the scaffold; no blocking calls (`.block()`, JDBC, `Thread.sleep`)
- [ ] Only the requested actions exist, with no extra operations, repository methods or DTOs
- [ ] Fixture and one integration test class per operation are added (unit tests only for authorization rules)
