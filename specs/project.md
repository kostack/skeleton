Generate a runnable project task tracker using Kotlin, Spring Boot WebFlux, coroutines, and Gradle Kotlin DSL.

API design
- The only user endpoint is GET /me. Return the current authenticated user’s id, name, and email. Resolve the user from the Spring Security principal. Do not expose /users, user creation, user listing, or lookup by user ID.
- Keep project and task routes flat. Use query parameters for filtering and PATCH on the item to update relationships. 

Entities
- User: id (Long), name (String), email (String, unique), roles (List<String>).
- Project: id (Long), name (String), description (String?), ownerId (Long, required), featuredTaskId (Long?, initially null).
- Task: id (Long), projectId (Long, required), title (String), status (TaskStatus, defaults to TODO), assigneeId (Long?, initially null).
- TaskStatus: TODO, IN_PROGRESS, DONE.

Endpoints
| Method | Path | Behavior |
|---|---|---|
| GET | /me | Return the authenticated user’s profile. |
| POST | /projects | Create a project from name and optional description. Set ownerId from the authenticated principal. Reject a client-supplied ownerId. |
| GET | /projects | List projects. Support an optional ownerId filter. |
| GET | /projects/{id} | Return the project, an owner summary, and its optional featured task summary. |
| PATCH | /projects/{id} | Update name, description, or featuredTaskId. |
| POST | /tasks | Create a task from projectId, title, and optional assigneeId. |
| GET | /tasks | List tasks. Support required projectId, and optional assigneeId, and status filters. Combine supplied filters with AND. |
| GET | /tasks/{id} | Return the task, its projectId, and an optional assignee summary. |
| PATCH | /tasks/{id} | Update title, status, or assigneeId. |

Relationship rules
- 1:N — One user can own multiple projects. Every project has one owner.
- 1:N — One project can contain multiple tasks. Every task belongs to one existing project.
- N:1 — Multiple tasks can share an assignee. A task may be unassigned. A supplied assigneeId must reference an existing user.
- Optional 1:1 — A project may feature one of its own tasks. A task can therefore be featured by at most one project. Clearing or replacing the selection updates only the project.
- Owner and assignee summaries contain only id and name. Email is exposed only through GET /me.


