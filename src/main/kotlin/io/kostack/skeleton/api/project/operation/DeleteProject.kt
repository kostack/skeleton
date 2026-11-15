package io.kostack.skeleton.api.project.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus

@ProjectOperation
class DeleteProject(
  private val projectRepository: ProjectRepository,
  private val taskRepository: TaskRepository,
  private val transactionalOperator: TransactionalOperator
) {
  @Operation(
    operationId = "project_delete_action",
    summary = "Delete a project and its tasks",
    description = "Only the owner may delete a project. Its tasks are deleted in the same transaction."
  )
  @DeleteMapping("/{projectId}", version = "1+")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@projectAuth.requireOwner(#project, authentication.principal)")
  suspend operator fun invoke(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project
  ) {
    val id = requireNotNull(project.id)
    transactionalOperator.executeAndAwait {
      // Concurrent task inserts wait for this lock; a project already deleted concurrently needs nothing more
      projectRepository.findByIdForUpdate(id) ?: return@executeAndAwait
      taskRepository.deleteAllByProjectId(id)
      projectRepository.deleteById(id)
    }
  }
}
