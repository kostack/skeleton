package io.kostack.skeleton.service.task

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.task.filter.TaskListFilter
import io.kostack.skeleton.controller.task.response.TaskListResponse
import io.kostack.skeleton.controller.task.response.TaskResponse
import io.kostack.skeleton.controller.task.response.toResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TaskServiceImpl(
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val userRepository: UserRepository,
  private val queryBuilder: GridQueryBuilder
) : TaskService {
  override suspend fun list(
    pageable: Pageable,
    filter: TaskListFilter,
    project: Project
  ): Pagination<TaskListResponse> {
    val pageRequest = pageable.toPageRequest(TaskListFilter::class)
    val taskTable = table<Task>("t")
    val projectTable = table<Project>("p")
    return queryBuilder
      .from(taskTable)
      .join(projectTable)
      .on(taskTable[Task::projectId] eq projectTable[Project::id])
      .where(taskTable[Task::projectId] eq requireNotNull(project.id))
      .fetchPage<TaskListResponse>(filter, pageRequest)
  }

  override suspend fun get(task: Task): TaskResponse {
    val project =
      checkNotNull(projectRepository.findById(task.projectId)) { "Project of task ${task.identifier} is missing" }
    val assignee = task.assigneeId?.let { userRepository.findByIdentity(it) }
    return task.toResponse(project = project, assignee = assignee)
  }

  override suspend fun create(
    projectIdentifier: String,
    title: String,
    assigneeId: String?
  ): TaskResponse {
    val project =
      projectRepository.findByIdentifier(projectIdentifier)
        ?: throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "projectId", message = "Project does not exist"))
        )
    val assignee =
      assigneeId?.let { assigneeId ->
        userRepository.findByIdentity(assigneeId)
          ?: throw ValidationException(
            listOf(ApiValidation.Violation(fieldId = "assigneeId", message = "User does not exist"))
          )
      }
    val task =
      try {
        taskRepository.save(
          Task(
            projectId = requireNotNull(project.id),
            title = title,
            assigneeId = assignee?.identity
          )
        )
      } catch (_: DataIntegrityViolationException) {
        // The project was deleted after it was loaded: the foreign key rejects the task
        throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "projectId", message = "Project does not exist"))
        )
      }
    return task.toResponse(project = project, assignee = assignee)
  }

  override suspend fun update(
    task: Task,
    title: String,
    status: TaskStatus,
    assigneeIdentity: String?
  ) {
    val assigneeId = assigneeIdentity?.let { existingUser(it) }
    val updated =
      task.copy(
        title = title,
        status = status,
        assigneeId = assigneeId
      )
    if (updated == task) {
      return
    }
    taskRepository.save(updated)
  }

  private suspend fun existingUser(identity: String): String =
    userRepository.findByIdentity(identity)?.identity
      ?: throw ValidationException(
        listOf(ApiValidation.Violation(fieldId = "assigneeId", message = "User does not exist"))
      )
}
