package io.kostack.skeleton.db.mongodb.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.kostack.jwt_security.dto.JwtUser
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
data class User(
  @Id val id: String? = null,
  @Indexed(unique = true) override val identity: String,
  val roles: List<String>,
  @JsonIgnore @Version val version: Long? = null
) : JwtUser {
  override fun claims(): Map<String, Any> =
    buildMap {
      put("identity", identity)
      put("roles", roles)
    }
}
