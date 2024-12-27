package com.sothrose.assetflow_market_data_aggregator.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "market_data")
public class MarketData {
  @Id private String id;
  private String symbol;
  private AssetType assetType;
  private BigDecimal price;
  private String currency;
  private String source;
  private LocalDateTime timestamp;
}
