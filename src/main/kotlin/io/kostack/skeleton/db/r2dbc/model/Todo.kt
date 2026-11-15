package io.kostack.skeleton.db.r2dbc.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("todos")
data class Todo(
  @Id val id: Long? = null,
  val identifier: String,
  val title: String,
  val completed: Boolean = false,
  val projectId: Long,
  @CreatedDate val createdAt: Instant = Instant.now(),
  @LastModifiedDate val lastModifiedAt: Instant = Instant.now(),
  val createdBy: String,
  @Version val version: Long? = null
)
