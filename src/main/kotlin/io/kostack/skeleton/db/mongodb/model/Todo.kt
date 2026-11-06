package io.kostack.skeleton.db.mongodb.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Serializable
@Document("todos")
data class Todo(
  @Id val id: String,
  @Indexed(unique = true) val title: String,
  @Indexed val completed: Boolean = false,
  @Indexed @Contextual @CreatedDate val createdAt: Instant = Instant.now(),
  @Indexed val createdBy: String
)
