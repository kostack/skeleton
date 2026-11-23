package io.kostack.skeleton.db.r2dbc.repository

import io.kostack.skeleton.db.r2dbc.model.Project
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : CoroutineCrudRepository<Project, Long> {
  suspend fun findByIdentifier(identifier: String): Project?

  @Query("SELECT * FROM projects WHERE id = :id FOR UPDATE")
  suspend fun findByIdForUpdate(id: Long): Project?
}
