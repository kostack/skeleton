package io.kostack.skeleton.security

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UserProviderImplTest {
  private val userRepository = mockk<UserRepository>()
  private val userProvider = UserProviderImpl(userRepository)

  @Test
  fun `load returns the stored user and ignores the roles claim`() =
    runTest {
      val stored =
        User(
          id = "user-id-1",
          identity = "user-1",
          name = "Alice",
          email = "user-1@example.com",
          roles = listOf("ROLE_USER")
        )
      coEvery { userRepository.findByIdentity("user-1") } returns stored

      val result = userProvider.load("user-1", mapOf("roles" to listOf("ROLE_ADMIN")))

      assertEquals(stored, result)
    }

  @Test
  fun `load builds a user from the token claims when the user is not stored`() =
    runTest {
      coEvery { userRepository.findByIdentity("user-9") } returns null

      val result =
        userProvider.load(
          "user-9",
          mapOf("name" to "Nina", "email" to "user-9@example.com", "roles" to listOf("ROLE_USER"))
        )

      val user = assertIs<User>(result)
      assertNull(user.id)
      assertEquals("user-9", user.identity)
      assertEquals("Nina", user.name)
      assertEquals("user-9@example.com", user.email)
      assertEquals(listOf("ROLE_USER"), user.roles)
    }

  @Test
  fun `load rejects an unknown user whose token has no roles claim`() =
    runTest {
      coEvery { userRepository.findByIdentity("user-9") } returns null

      assertThrows<IllegalArgumentException> {
        userProvider.load("user-9", emptyMap())
      }
    }
}
