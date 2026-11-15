package io.kostack.skeleton.db.r2dbc.repository

import io.kostack.skeleton.db.r2dbc.model.Todo
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : CoroutineCrudRepository<Todo, Long> {
  suspend fun findByIdentifier(identifier: String): Todo?

  suspend fun findByIdentifierAndCreatedBy(
    identifier: String,
    createdBy: String
  ): Todo?

  fun findAllByProjectIdAndCreatedByOrderByCreatedAt(
    projectId: Long,
    createdBy: String
  ): Flow<Todo>
}
