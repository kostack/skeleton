package io.kostack.skeleton.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.rabbitmq.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
  @Bean
  @ServiceConnection
  fun mysqlContainer(): MySQLContainer =
    MySQLContainer(DockerImageName.parse("mysql:8.4"))
      .withDatabaseName("skeleton")

  @Bean
  @ServiceConnection
  fun mongoDbContainer(): MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:8.0"))

  @Bean
  @ServiceConnection
  fun rabbitMqContainer(): RabbitMQContainer = RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-management"))
}
