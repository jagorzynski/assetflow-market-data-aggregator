package com.sothrose.assetflow_market_data_aggregator.exception;

public class InvalidCryptoSymbolException extends RuntimeException {
  public InvalidCryptoSymbolException(String message) {
    super(message);
  }
}
