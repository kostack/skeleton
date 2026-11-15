package io.kostack.skeleton.db.r2dbc.repository

import io.kostack.skeleton.db.r2dbc.model.Task
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : CoroutineCrudRepository<Task, Long> {
  suspend fun findByIdentifier(identifier: String): Task?

  @Modifying
  @Query("DELETE FROM tasks WHERE project_id = :projectId")
  suspend fun deleteAllByProjectId(projectId: Long): Long
}
