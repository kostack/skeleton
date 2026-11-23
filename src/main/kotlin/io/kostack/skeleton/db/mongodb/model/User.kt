package io.kostack.skeleton.db.mongodb.model

import io.github.kostack.jwt_security.dto.JwtUser
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
data class User(
  @Id val id: String? = null,
  @Indexed(unique = true) override val identity: String,
  val name: String,
  @Indexed(unique = true) val email: String,
  val roles: List<String>,
  @Version val version: Long? = null
) : JwtUser {
  override fun claims(): Map<String, Any> =
    buildMap {
      put("identity", identity)
      put("name", name)
      put("email", email)
      put("roles", roles)
    }
}
