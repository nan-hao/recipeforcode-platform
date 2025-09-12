package com.recipeforcode.platform.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.retry.Retry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@AutoConfiguration
@ConditionalOnClass({CircuitBreaker.class, Retry.class})
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "recipeforcodeDefaultCircuitBreakerCustomizer")
    public CircuitBreakerConfigCustomizer recipeforcodeDefaultCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer.of("default", builder -> builder
            .slidingWindowSize(50)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(5))
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "recipeforcodeDefaultRetryCustomizer")
    public RetryConfigCustomizer recipeforcodeDefaultRetryCustomizer() {
        return RetryConfigCustomizer.of("default", builder -> builder
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(200))
        );
    }
}
