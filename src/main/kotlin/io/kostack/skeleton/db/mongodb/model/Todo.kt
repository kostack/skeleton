package io.kostack.skeleton.db.mongodb.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("todos")
data class Todo(
  @Id val id: String,
  @Indexed(unique = true) val title: String,
  @Indexed val completed: Boolean = false,
  @Indexed @CreatedDate val createdAt: Instant = Instant.now(),
  @LastModifiedDate val lastModifiedAt: Instant = Instant.now(),
  @Indexed val createdBy: String,
  @Version val version: String? = null
)
