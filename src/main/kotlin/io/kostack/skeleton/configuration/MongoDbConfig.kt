package io.kostack.skeleton.configuration

import io.github.kostack.fixtures.FixtureLoadEvent
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import reactor.core.publisher.Flux

@Configuration
@EnableReactiveMongoAuditing
@EnableReactiveMongoRepositories(basePackages = ["io.kostack.skeleton.db.mongodb.repository"])
class MongoDbConfig(
  private val mongoTemplate: ReactiveMongoTemplate,
  private val mongoMappingContext: MongoMappingContext
) {
  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    initIndices()
  }

  @EventListener(FixtureLoadEvent::class)
  fun onFixtureLoad() {
    initIndices()
  }

  fun initIndices() {
    val resolver = MongoPersistentEntityIndexResolver(mongoMappingContext)
    Flux
      .fromIterable(
        mongoMappingContext.persistentEntities
          .filter { it.isAnnotationPresent(Document::class.java) }
      ).flatMap { entity ->
        val indexOps = mongoTemplate.indexOps(entity.type)
        Flux
          .fromIterable(resolver.resolveIndexFor(entity.type))
          .flatMap { indexDefinition -> indexOps.createIndex(indexDefinition) }
      }.blockLast()
  }
}
