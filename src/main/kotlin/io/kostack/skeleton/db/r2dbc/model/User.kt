package io.kostack.skeleton.db.r2dbc.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.kostack.jwt_security.dto.JwtUser
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
  @Id val id: Long? = null,
  override val identity: String,
  val roles: List<String>,
  @JsonIgnore @Version val version: Long? = null
) : JwtUser {
  override fun claims(): Map<String, Any> =
    buildMap {
      put("identity", identity)
      put("roles", roles)
    }
}
