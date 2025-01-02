package com.sothrose.assetflow_market_data_aggregator.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidCryptoSymbolException.class)
  public ResponseEntity<String> handleInvalidCryptoSymbolException(
      InvalidCryptoSymbolException ex) {
    return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
  }

  @ExceptionHandler(ExternalApiException.class)
  public ResponseEntity<String> handleExternalApiException(ExternalApiException ex) {
    return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGeneralException(Exception ex) {
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body("An unexpected error occurred: " + ex.getMessage());
  }
}
