package com.sothrose.assetflow_market_data_aggregator.repository;

import com.sothrose.assetflow_market_data_aggregator.model.AssetType;
import com.sothrose.assetflow_market_data_aggregator.model.MarketData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MarketDataRepository extends ReactiveMongoRepository<MarketData, String> {
  Flux<MarketData> findByAssetType(AssetType assetType);

  Flux<MarketData> findBySymbol(String symbol);
}
