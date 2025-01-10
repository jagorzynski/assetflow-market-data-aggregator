package com.sothrose.assetflow_market_data_aggregator.service;

import static java.time.Duration.ofMinutes;
import static java.util.Objects.nonNull;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Flux.interval;
import static reactor.core.publisher.Mono.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sothrose.assetflow_market_data_aggregator.exception.ExternalApiException;
import com.sothrose.assetflow_market_data_aggregator.exception.InvalidCryptoSymbolException;
import com.sothrose.assetflow_market_data_aggregator.model.CryptoDataDto;
import com.sothrose.assetflow_market_data_aggregator.model.MarketData;
import com.sothrose.assetflow_market_data_aggregator.repository.MarketDataRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.Disposable;
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
  public static final String USD = "usd";
  public static final String REDIS_CRYPTO_KEY_PART = "crypto:";
  public static final String COLON = ":";
  private final ReactiveRedisTemplate<String, String> customRedisTemplate;
  private final WebClient coinGeckoWebClient;
  private final MarketDataRepository marketDataRepository;
  private final ObjectMapper objectMapper;
  private Disposable marketDataFetchingTask;

  @PostConstruct
  public void initializeMarketDataFetching() {
    marketDataFetchingTask =
        interval(ofMinutes(1))
            .flatMap(tick -> fetchAndStoreMarketData())
            .onErrorContinue(
                (error, obj) -> {
                  log.error("Error in scheduled task: {}", error.getMessage());
                  // FIXME retry?
                })
            .subscribe();
  }

  @PreDestroy
  public void cleanup() {
    var isNotNullAndDisposedTask =
        nonNull(marketDataFetchingTask) && !marketDataFetchingTask.isDisposed();
    if (isNotNullAndDisposedTask) {
      marketDataFetchingTask.dispose();
    }
  }

  public Mono<MarketData> fetchAndCacheCryptoData(String coinId, String currency) {
    var redisKey = generateRedisKey(coinId, currency);
    return fetchFromCache(redisKey)
        .switchIfEmpty(fetchFromApiAndCache(coinId, currency, redisKey))
        .flatMap(dto -> saveToRepository(dto, currency))
        .doOnError(
            ex ->
                log.error(
                    "Error occurred while fetching or caching data for [{}]: [{}]",
                    coinId,
                    ex.getMessage()));
  }

  public Flux<MarketData> fetchMultipleCryptoPrice(List<String> symbols, String currency) {
    return fromIterable(symbols)
        .flatMap(symbol -> fetchAndCacheCryptoData(symbol, currency))
        .doOnError(
            ex ->
                log.error(
                    "Error occurred while fetching or caching multiple cryptos: [{}]",
                    ex.getMessage()));
  }

  public Flux<MarketData> fetchHistoricalMarketData() {
    return marketDataRepository.findAll();
  }

  private Mono<MarketData> saveToRepository(CryptoDataDto dto, String currency) {
    return marketDataRepository.save(dto.toMarketData(currency, COIN_GECKO));
  }

  // FIXME
  private Flux<MarketData> fetchAndStoreMarketData() {
    var coins = List.of("bitcoin", "ethereum", "litecoin");
    return fromIterable(coins)
        .flatMap(coin -> fetchAndCacheCryptoData(coin, USD))
        .doOnError(ex -> log.error("Error in market data fetching task: {}", ex.getMessage()));
  }

  private String generateRedisKey(String coinId, String currency) {
    return REDIS_CRYPTO_KEY_PART + coinId + COLON + currency;
  }

  private Mono<CryptoDataDto> fetchFromCache(String redisKey) {
    return customRedisTemplate
        .opsForValue()
        .get(redisKey)
        .flatMap(cachedData -> deserializeCachedData(cachedData, redisKey));
  }

  private Mono<CryptoDataDto> deserializeCachedData(String cachedData, String redisKey) {
    try {
      var dto = objectMapper.readValue(cachedData, CryptoDataDto.class);
      return just(dto);
    } catch (JsonProcessingException e) {
      log.error("Error occurred when parsing cached data for key {}: {}", redisKey, e.getMessage());
      return empty();
    }
  }

  private Mono<CryptoDataDto> fetchFromApiAndCache(
      String coinId, String currency, String redisKey) {
    return coinGeckoWebClient
        .get()
        .uri(uriBuilder -> buildCoinGeckoUri(uriBuilder, coinId, currency))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        errorMessage ->
                            error(
                                new InvalidCryptoSymbolException(
                                    "Invalid symbol or unsupported currency: " + errorMessage))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(
                        errorMessage ->
                            error(
                                new ExternalApiException("CoinGecko API error: " + errorMessage))))
        .bodyToFlux(CryptoDataDto.class)
        .next()
        .flatMap(dto -> saveToCache(dto, redisKey))
        .doOnError(
            ex ->
                log.error(
                    "Error during API fetch or cache save for coin [{}] with currency [{}]: [{}]",
                    coinId,
                    currency,
                    ex.getMessage()));
  }

  private URI buildCoinGeckoUri(UriBuilder uriBuilder, String coinId, String currency) {
    return uriBuilder
        .path(COIN_GECKO_COINS_MARKETS_PATH)
        .queryParam(VS_CURRENCY, currency)
        .queryParam(IDS, coinId)
        .build();
  }

  private Mono<CryptoDataDto> saveToCache(CryptoDataDto dto, String redisKey) {
    try {
      var dtoJson = objectMapper.writeValueAsString(dto);
      return customRedisTemplate.opsForValue().set(redisKey, dtoJson, ofMinutes(2)).thenReturn(dto);
    } catch (JsonProcessingException e) {
      log.error(
          "Error occurred when serializing CryptoDataDto for key [{}]: [{}]",
          redisKey,
          e.getMessage());
      return just(dto);
    }
  }
}
