package com.sothrose.assetflow_market_data_aggregator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  public static final String COIN_GECKO_URL = "https://api.coingecko.com/api/v3";

  @Bean(name = "coinGeckoWebClient")
  public WebClient coinGeckoWebClient() {
    return WebClient.builder().baseUrl(COIN_GECKO_URL).build();
  }
}
