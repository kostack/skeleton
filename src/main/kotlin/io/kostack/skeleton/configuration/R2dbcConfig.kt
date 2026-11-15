package io.kostack.skeleton.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

@Configuration
@EnableR2dbcRepositories(basePackages = ["io.kostack.skeleton.db.r2dbc.repository"])
@EnableR2dbcAuditing(dateTimeProviderRef = R2dbcConfig.AUDITING_DATE_TIME_PROVIDER)
class R2dbcConfig {
  companion object {
    const val AUDITING_DATE_TIME_PROVIDER = "r2dbcAuditingDateTimeProvider"
  }

  @Bean(AUDITING_DATE_TIME_PROVIDER)
  fun r2dbcAuditingDateTimeProvider(): DateTimeProvider =
    DateTimeProvider { Optional.of(Instant.now().truncatedTo(ChronoUnit.MICROS)) }
}
