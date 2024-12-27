package com.sothrose.assetflow_market_data_aggregator;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableMongock
@SpringBootApplication
public class AssetflowMarketDataAggregatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssetflowMarketDataAggregatorApplication.class, args);
	}

}
