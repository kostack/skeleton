package io.kostack.skeleton.integration

import io.github.kostack.fixtures.DataFixtureManager
import io.github.kostack.jwt_security.autoconfigure.JwtProperties
import io.github.kostack.jwt_security.service.JwtManager
import io.github.kostack.jwt_security.service.JwtUser
import io.kostack.skeleton.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class BaseControllerIntegrationTest {
  @LocalServerPort
  protected var port: Int = 0

  @Autowired
  protected lateinit var jwtManager: JwtManager

  @Autowired
  protected lateinit var userService: UserService

  @Autowired
  protected lateinit var jwtProperties: JwtProperties

  @Autowired
  protected lateinit var dataFixtureManager: DataFixtureManager

  protected fun webTestClient(): WebTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

  protected suspend fun user(identity: String): JwtUser {
    val u = userService.findByIdentity(identity)
    return requireNotNull(u)
  }

  protected suspend fun authorization(identity: String): String {
    val prefix = headerPrefix()
    val token = jwtManager.generateToken(user(identity))
    return prefix + token
  }

  private fun headerPrefix(): String {
    var prefix = jwtProperties.authorizationHeaderOptions.prefix
    if (prefix.isNotEmpty()) prefix += " "
    return prefix
  }
}
