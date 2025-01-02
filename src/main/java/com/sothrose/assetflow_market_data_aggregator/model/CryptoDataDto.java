package com.sothrose.assetflow_market_data_aggregator.model;

import static java.time.LocalDateTime.now;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CryptoDataDto {
  private String id;
  private String symbol;
  private String name;

  @JsonProperty("current_price")
  private BigDecimal currentPrice;

  @JsonProperty("market_cap")
  private BigDecimal marketCap;

  @JsonProperty("total_volume")
  private BigDecimal totalVolume;

  @JsonProperty("high_24h")
  private BigDecimal high24h;

  @JsonProperty("low_24h")
  private BigDecimal low24h;

  public MarketData toMarketData(String currency, String source) {
    return MarketData.builder()
        .symbol(symbol)
        .assetType(AssetType.CRYPTO)
        .price(currentPrice)
        .currency(currency)
        .source(source)
        .timestamp(now())
        .marketCap(marketCap)
        .volume24h(totalVolume)
        .build();
  }
}
