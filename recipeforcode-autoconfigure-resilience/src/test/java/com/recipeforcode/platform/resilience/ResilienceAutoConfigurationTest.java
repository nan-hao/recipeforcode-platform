package com.recipeforcode.platform.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ResilienceAutoConfiguration.class));

    @Test
    void shouldLoadContext() {
        contextRunner.run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void shouldApplyDefaultCircuitBreakerCustomizer() {
        contextRunner.run(ctx -> {
            var customizer = ctx.getBean("recipeforcodeDefaultCircuitBreakerCustomizer",
                io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer.class);
            var builder = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowSize(10) // will be overridden
                .failureRateThreshold(10f);
            customizer.customize(builder);
            var config = builder.build();
            assertThat(config.getSlidingWindowSize()).isEqualTo(50);
            assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
            var intervalFn = config.getWaitIntervalFunctionInOpenState();
            assertThat(intervalFn).isNotNull();
            assertThat(intervalFn.apply(1)).isEqualTo(5000L);
        });
    }

    @Test
    void shouldApplyDefaultRetryCustomizer() {
        contextRunner.run(ctx -> {
            var customizer = ctx.getBean("recipeforcodeDefaultRetryCustomizer",
                io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer.class);
            var builder = io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(1)
                .waitDuration(java.time.Duration.ofMillis(1));
            customizer.customize(builder);
            var config = builder.build();
            assertThat(config.getMaxAttempts()).isEqualTo(3);
            assertThat(config.getIntervalBiFunction()).isNotNull();
        });
    }
}
