package com.sothrose.assetflow_market_data_aggregator.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.sothrose.assetflow_market_data_aggregator.model.MarketData;
import com.sothrose.assetflow_market_data_aggregator.service.MarketDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/v1/assetflow/crypto")
public class MarketDataController {

  private final MarketDataService marketDataService;

  @GetMapping(path = "/single", produces = APPLICATION_JSON_VALUE)
  public Mono<MarketData> getSingleCryptoPrice(
      @RequestParam String symbol, @RequestParam String currency) {
    return marketDataService.fetchSingleCryptoPrice(symbol, currency);
  }

  @GetMapping(path = "/multiple", produces = APPLICATION_JSON_VALUE)
  public Flux<MarketData> getMultipleCryptoPrice(
      @RequestParam List<String> symbols, @RequestParam String currency) {
    return marketDataService.fetchMultipleCryptoPrice(symbols, currency);
  }

  @GetMapping(path = "/data/all", produces = APPLICATION_JSON_VALUE)
  public Flux<MarketData> getHistoricalMarketData() {
    return marketDataService.fetchHistoricalMarketData();
  }
}
