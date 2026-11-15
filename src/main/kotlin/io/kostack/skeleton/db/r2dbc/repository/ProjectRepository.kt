package io.kostack.skeleton.db.r2dbc.repository

import io.kostack.skeleton.db.r2dbc.model.Project
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : CoroutineCrudRepository<Project, Long> {
  suspend fun findByIdentifier(identifier: String): Project?
}
