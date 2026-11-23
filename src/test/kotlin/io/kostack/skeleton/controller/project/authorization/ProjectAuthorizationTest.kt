package io.kostack.skeleton.controller.project.authorization

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectAuthorizationTest {
  private val authorization = ProjectAuthorization()
  private val project = Project(name = "Website", ownerId = "user-1")

  private fun user(identity: String) =
    User(identity = identity, name = identity, email = "$identity@example.com", roles = listOf("ROLE_USER"))

  @Test
  fun `owner is allowed`() =
    runTest {
      assertTrue(authorization.requireOwner(project, user("user-1")).awaitSingle())
    }

  @Test
  fun `other user is denied`() =
    runTest {
      assertFalse(authorization.requireOwner(project, user("user-2")).awaitSingle())
    }
}
