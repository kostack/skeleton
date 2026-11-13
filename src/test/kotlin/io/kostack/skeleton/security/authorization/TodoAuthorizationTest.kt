package io.kostack.skeleton.security.authorization

import io.kostack.skeleton.controller.me.authorization.DefaultAuthorization
import io.kostack.skeleton.db.r2dbc.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class TodoAuthorizationTest {
  @Autowired
  lateinit var defaultAuthorization: DefaultAuthorization

  @Test
  fun `canView returns true when user has admin role`() =
    runTest {
      // Arrange
      val mockUser = mockk<User>()
      every { mockUser.hasRole("ROLE_ADMIN") } returns true

      // Act
      val result = defaultAuthorization.canView(mockUser).awaitSingle()

      verify { mockUser.hasRole("ROLE_ADMIN") }

      // Assert
      assertTrue(result)
    }

  @Test
  fun `canView returns false when user does not have admin role`() =
    runTest {
      // Arrange
      val mockUser = mockk<User>()
      every { mockUser.hasRole("ROLE_ADMIN") } returns false

      // Act
      val result = defaultAuthorization.canView(mockUser).awaitSingle()

      // Assert
      assertFalse(result)
    }
}
