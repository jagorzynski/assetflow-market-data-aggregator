package com.sothrose.assetflow_market_data_aggregator.service;

import static java.lang.String.join;

import com.sothrose.assetflow_market_data_aggregator.exception.ExternalApiException;
import com.sothrose.assetflow_market_data_aggregator.exception.InvalidCryptoSymbolException;
import com.sothrose.assetflow_market_data_aggregator.model.CryptoDataDto;
import com.sothrose.assetflow_market_data_aggregator.model.MarketData;
import com.sothrose.assetflow_market_data_aggregator.repository.MarketDataRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class MarketDataService {

  public static final String DELIMITER = ",";
  public static final String COIN_GECKO = "CoinGecko";
  public static final String VS_CURRENCY = "vs_currency";
  public static final String IDS = "ids";
  public static final String COIN_GECKO_COINS_MARKETS_PATH = "/coins/markets";
  private final WebClient coinGeckoWebClient;
  private final MarketDataRepository marketDataRepository;

  public Mono<MarketData> fetchSingleCryptoPrice(String symbol, String currency) {
    return coinGeckoWebClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(COIN_GECKO_COINS_MARKETS_PATH)
                    .queryParam(VS_CURRENCY, currency)
                    .queryParam(IDS, symbol)
                    .build())
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        errorMessage ->
                            Mono.error(
                                new InvalidCryptoSymbolException(
                                    "Invalid symbol or unsupported currency: " + errorMessage))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        errorMessage ->
                            Mono.error(
                                new ExternalApiException("CoinGecko API error: " + errorMessage))))
        .bodyToFlux(CryptoDataDto.class)
        .next()
        .map(dto -> dto.toMarketData(currency, COIN_GECKO))
        .flatMap(marketDataRepository::save)
        .doOnError(
            ex -> log.error("Error while fetching or saving single crypto: {}", ex.getMessage()));
  }

  public Flux<MarketData> fetchMultipleCryptoPrice(List<String> symbols, String currency) {
    return coinGeckoWebClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(COIN_GECKO_COINS_MARKETS_PATH)
                    .queryParam(VS_CURRENCY, currency)
                    .queryParam(IDS, join(DELIMITER, symbols))
                    .build())
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        errorMessage ->
                            Mono.error(
                                new InvalidCryptoSymbolException(
                                    "Invalid symbols or unsupported currency: " + errorMessage))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        errorMessage ->
                            Mono.error(
                                new ExternalApiException("CoinGecko API error: " + errorMessage))))
        .bodyToFlux(CryptoDataDto.class)
        .map(dto -> dto.toMarketData(currency, COIN_GECKO))
        .flatMap(marketDataRepository::save)
        .doOnError(
            ex ->
                log.error("Error while fetching or saving multiple cryptos: {}", ex.getMessage()));
  }

  public Flux<MarketData> fetchHistoricalMarketData() {
    return marketDataRepository.findAll();
  }
}
