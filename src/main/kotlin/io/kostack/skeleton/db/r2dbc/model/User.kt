package io.kostack.skeleton.db.r2dbc.model

import io.github.kostack.jwt_security.service.JwtUser
import kotlinx.serialization.Transient
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
  @Id
  val id: Long? = null,
  override val identity: String,
  var username: String,
  @Transient
  var roles: List<String> = listOf()
) : JwtUser {
  fun hasRole(role: String): Boolean = roles.contains(role)

  override fun toMap(): MutableMap<String, Any> {
    val map: MutableMap<String, Any> = mutableMapOf()
    map["identity"] = identity
    map["username"] = username
    map["roles"] = roles
    return map
  }
}
