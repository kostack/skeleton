package io.kostack.skeleton.configuration

import io.github.kostack.database_tools.r2dbc.converter.ListToStringConverter
import io.github.kostack.database_tools.r2dbc.converter.StringToListConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.MySqlDialect
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackages = ["io.kostack.skeleton.db.r2dbc.repository"])
class R2dbcConfig {
  @Bean
  fun r2dbcCustomConversions(): R2dbcCustomConversions =
    R2dbcCustomConversions.of(
      MySqlDialect.INSTANCE,
      listOf(
        StringToListConverter(
          json = json,
          serializer = ListSerializer(String.serializer())
        ),
        ListToStringConverter()
      )
    )

  companion object {
    private val json =
      Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
      }
  }
}
