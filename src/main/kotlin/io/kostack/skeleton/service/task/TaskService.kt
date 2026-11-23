package io.kostack.skeleton.service.task

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.task.filter.TaskListFilter
import io.kostack.skeleton.controller.task.response.TaskListResponse
import io.kostack.skeleton.controller.task.response.TaskResponse
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import org.springframework.data.domain.Pageable

interface TaskService {
  suspend fun list(
    pageable: Pageable,
    filter: TaskListFilter,
    project: Project
  ): Pagination<TaskListResponse>

  suspend fun get(task: Task): TaskResponse

  suspend fun create(
    projectIdentifier: String,
    title: String,
    assigneeId: String?
  ): TaskResponse

  suspend fun update(
    task: Task,
    title: String,
    status: TaskStatus,
    assigneeIdentity: String?
  )
}
