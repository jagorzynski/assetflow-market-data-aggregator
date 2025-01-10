package com.sothrose.assetflow_market_data_aggregator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

  @Bean(name = "customRedisTemplate")
  public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    var serializationContext =
        RedisSerializationContext.<String, String>newSerializationContext(RedisSerializer.string())
            .build();
    return new ReactiveRedisTemplate<>(factory, serializationContext);
  }
}
