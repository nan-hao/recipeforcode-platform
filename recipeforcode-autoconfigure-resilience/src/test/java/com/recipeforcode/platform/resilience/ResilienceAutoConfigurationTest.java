package com.recipeforcode.platform.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ResilienceAutoConfiguration.class));

    @Test
    void contextLoads() {
        contextRunner.run(ctx -> assertThat(ctx).hasNotFailed());
    }
}

