package io.kostack.skeleton.db.r2dbc.repository

import io.kostack.skeleton.db.r2dbc.model.Task
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : CoroutineCrudRepository<Task, Long> {
  suspend fun findByIdentifier(identifier: String): Task?
}
