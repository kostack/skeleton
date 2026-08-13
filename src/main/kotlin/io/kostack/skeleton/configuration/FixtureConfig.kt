package io.kostack.skeleton.configuration

import io.github.kostack.database_tools.purger.MongoDbPurger
import io.github.kostack.database_tools.purger.R2dbcDbPurger
import io.github.kostack.fixtures.Purger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("dev", "test")
class FixtureConfig(
  private val mongodbPurger: MongoDbPurger,
  private val r2dbcDbPurger: R2dbcDbPurger
) {
  @Bean
  fun registerPurger(): Purger = PurgerAdapter(mongodbPurger, r2dbcDbPurger)

  private class PurgerAdapter(
    private val mongoDbPurger: MongoDbPurger,
    private val r2dbcDbPurger: R2dbcDbPurger
  ) : Purger {
    override suspend fun purge() {
      mongoDbPurger.purge()
      r2dbcDbPurger.purge()
    }
  }
}
