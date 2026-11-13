package io.kostack.skeleton.security

import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.user.UserService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserProviderImplTest {
  private val userService = mockk<UserService>()
  private val userProvider = UserProviderImpl(userService)

  @Test
  fun `load returns the stored user and ignores the roles claim`() =
    runTest {
      val stored = User(id = 1, identity = "user-1", roles = listOf("ROLE_USER"))
      coEvery { userService.findByIdentity("user-1") } returns stored

      val result = userProvider.load("user-1", mapOf("roles" to listOf("ROLE_ADMIN")))

      assertEquals(stored, result)
    }

  @Test
  fun `load builds a user from the token claims when the user is not stored`() =
    runTest {
      coEvery { userService.findByIdentity("user-9") } returns null

      val result = userProvider.load("user-9", mapOf("roles" to listOf("ROLE_USER")))

      val user = assertIs<User>(result)
      assertEquals("user-9", user.identity)
      assertEquals(listOf("ROLE_USER"), user.roles)
    }

  @Test
  fun `load rejects an unknown user whose token has no roles claim`() =
    runTest {
      coEvery { userService.findByIdentity("user-9") } returns null

      assertThrows<IllegalArgumentException> {
        userProvider.load("user-9", emptyMap())
      }
    }
}
