package io.kostack.skeleton.integration

import io.github.kostack.fixtures.DataFixtureManager
import io.github.kostack.jwt_security.autoconfigure.SecurityProperties
import io.github.kostack.jwt_security.encoder.JwtEncoder
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.UserService
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
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

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var encoder: JwtEncoder

  @Autowired
  private lateinit var jwtProperties: SecurityProperties

  @Autowired
  protected lateinit var dataFixtureManager: DataFixtureManager

  @Autowired
  private lateinit var flyway: Flyway

  protected val webTestClient: WebTestClient by lazy {
    WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
  }

  @BeforeEach
  fun loadFixtures() =
    runBlocking {
      cleanDatabase()
      dataFixtureManager.loadFixtures(true)
    }

  @AfterEach
  fun cleanDatabase() {
    // MySQL foreign-key checks are scoped to a connection
    flyway.configuration.dataSource.connection.use { connection ->
      connection.createStatement().use { statement ->
        val tables =
          statement
            .executeQuery(
              """
              SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_TYPE = 'BASE TABLE'
                AND TABLE_NAME != 'flyway_schema_history'
              """.trimIndent()
            ).use { result ->
              buildList {
                while (result.next()) add(result.getString("TABLE_NAME"))
              }
            }
        statement.execute("SET FOREIGN_KEY_CHECKS = 0")
        try {
          for (table in tables) {
            statement.execute("TRUNCATE TABLE `${table.replace("`", "``")}`")
          }
        } finally {
          statement.execute("SET FOREIGN_KEY_CHECKS = 1")
        }
      }
    }
  }

  protected suspend fun user(identity: String): User =
    requireNotNull(userService.findByIdentity(identity)) {
      "Test user not found: $identity. Load fixtures before authenticating."
    }

  protected suspend fun accessToken(username: String): String = encoder.encode(user(username))

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
}
