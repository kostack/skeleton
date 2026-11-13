package io.kostack.skeleton.integration

import io.github.kostack.fixtures.DataFixtureManager
import io.github.kostack.fixtures.ReferenceRepository
import io.github.kostack.jwt_security.autoconfigure.SecurityProperties
import io.github.kostack.jwt_security.encoder.JwtEncoder
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.UserService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.ResourceLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ResourceLock("controller-integration-database")
abstract class BaseControllerIntegrationTest {
  @LocalServerPort
  private var port: Int = 0

  @Autowired protected lateinit var references: ReferenceRepository

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var encoder: JwtEncoder

  @Autowired
  private lateinit var jwtProperties: SecurityProperties

  @Autowired
  protected lateinit var dataFixtureManager: DataFixtureManager

  protected val webTestClient: WebTestClient by lazy {
    WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
  }

  @BeforeEach
  fun loadFixtures() =
    runBlocking {
      dataFixtureManager.loadFixtures(true)
    }

  protected suspend fun user(identity: String): User =
    requireNotNull(userService.findByIdentity(identity)) {
      "Test user not found: $identity. Load fixtures before authenticating."
    }

  protected fun authorizationHeader(accessToken: String): Pair<String, String> {
    val header = jwtProperties.authorizationHeader
    val value =
      if (header.prefix.isEmpty()) {
        accessToken
      } else {
        "${header.prefix} $accessToken"
      }
    return header.name to value
  }

  protected suspend fun clientFor(user: User): WebTestClient {
    val (header, value) = authorizationHeader(encoder.encode(user))
    return webTestClient
      .mutate()
      .defaultHeader(header, value)
      .build()
  }
}
