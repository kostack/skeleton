package io.kostack.skeleton.db.mongodb.repository

import io.kostack.skeleton.db.mongodb.model.Todo
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : CoroutineCrudRepository<Todo, String> {
  suspend fun findByTitle(title: String): Todo?
}
