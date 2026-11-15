package io.kostack.skeleton.db.r2dbc.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("tasks")
data class Task(
  @Id val id: Long? = null,
  val identifier: String,
  val projectId: Long,
  val title: String,
  val status: TaskStatus = TaskStatus.TODO,
  val assigneeId: String? = null,
  @CreatedDate val createdAt: Instant = Instant.now(),
  @LastModifiedDate val lastModifiedAt: Instant = Instant.now(),
  @Version val version: Long? = null
)
