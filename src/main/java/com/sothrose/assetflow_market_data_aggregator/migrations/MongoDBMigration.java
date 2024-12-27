package com.sothrose.assetflow_market_data_aggregator.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import reactor.core.publisher.Mono;

@ChangeUnit(id = "mongo-db-initialization", order = "001", author = "sothrose")
public class MongoDBMigration {

  private final ReactiveMongoTemplate reactiveMongoTemplate;

  public MongoDBMigration(ReactiveMongoTemplate reactiveMongoTemplate) {
    this.reactiveMongoTemplate = reactiveMongoTemplate;
  }

  @Execution
  public void changeSet() {
    reactiveMongoTemplate
        .collectionExists("market_data")
        .flatMap(
            exists -> {
              if (!exists) {
                return reactiveMongoTemplate.createCollection("market_data");
              }
              return Mono.empty();
            })
        .then(
            reactiveMongoTemplate
                .indexOps("market_data")
                .ensureIndex(new Index().on("symbol", Sort.Direction.ASC).unique()))
        .block();
  }

  @RollbackExecution
  public void rollback() {
    reactiveMongoTemplate.dropCollection("market_data").block();
  }
}
