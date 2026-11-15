package io.kostack.skeleton.db.r2dbc.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("projects")
data class Project(
  @Id val id: Long? = null,
  val identifier: String = UUID.randomUUID().toString(),
  val name: String,
  val description: String? = null,
  val ownerId: String,
  val featuredTaskId: Long? = null,
  @CreatedDate val createdAt: Instant = Instant.now(),
  @LastModifiedDate val lastModifiedAt: Instant = Instant.now(),
  @Version val version: Long? = null
)
