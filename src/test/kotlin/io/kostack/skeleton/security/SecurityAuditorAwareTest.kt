package io.kostack.skeleton.security

import io.kostack.skeleton.db.r2dbc.model.User
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

class SecurityAuditorAwareTest {
  private val auditorAware = SecurityAuditorAware()

  @Test
  fun `current auditor is the identity of the authenticated user`() {
    val user = User(identity = "user-1", roles = listOf("ROLE_USER"))
    val authentication = TestingAuthenticationToken(user, null, "ROLE_USER")

    StepVerifier
      .create(
        auditorAware.currentAuditor
          .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
      ).expectNext("user-1")
      .expectComplete()
      .verify(TIMEOUT)
  }

  @Test
  fun `current auditor is empty when there is no security context`() {
    StepVerifier
      .create(auditorAware.currentAuditor)
      .expectComplete()
      .verify(TIMEOUT)
  }

  @Test
  fun `current auditor is empty when the security context has no authentication`() {
    StepVerifier
      .create(
        auditorAware.currentAuditor
          .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(SecurityContextImpl())))
      ).expectComplete()
      .verify(TIMEOUT)
  }

  @Test
  fun `current auditor is empty when the principal is not a user`() {
    val authentication =
      AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

    StepVerifier
      .create(
        auditorAware.currentAuditor
          .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
      ).expectComplete()
      .verify(TIMEOUT)
  }

  companion object {
    private val TIMEOUT = Duration.ofSeconds(5)
  }
}
