package com.sothrose.assetflow_market_data_aggregator.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@ChangeUnit(id = "mongo-db-initialization", order = "001", author = "sothrose")
public class MongoDBMigration {

  public static final String MARKET_DATA = "market_data";
  public static final String SYMBOL = "symbol";
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  @Execution
  public void changeSet() {
    reactiveMongoTemplate
        .collectionExists(MARKET_DATA)
        .flatMap(this::checkCollectionPresent)
        .then(
            reactiveMongoTemplate
                .indexOps(MARKET_DATA)
                .ensureIndex(new Index().on(SYMBOL, Sort.Direction.ASC).unique()))
        .block();
  }

  @RollbackExecution
  public void rollback() {
    reactiveMongoTemplate.dropCollection(MARKET_DATA).block();
  }

  private Mono<?> checkCollectionPresent(boolean exists) {
    if (!exists) {
      return reactiveMongoTemplate.createCollection(MARKET_DATA);
    }
    return Mono.empty();
  }
}
