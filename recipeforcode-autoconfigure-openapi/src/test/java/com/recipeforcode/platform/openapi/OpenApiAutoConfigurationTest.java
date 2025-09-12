package com.recipeforcode.platform.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration.class))
            .withUserConfiguration() // nothing custom
            .withPropertyValues("recipeforcode.openapi.enabled=true");

    @Test
    void createsOpenApiBeanWhenEnabled() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(OpenAPI.class);
            var oas = ctx.getBean(OpenAPI.class);
            assertThat(oas.getInfo().getTitle()).isNotNull(); // defaults applied by props
        });
    }

    @Test
    void doesNotCreateBeanWhenDisabled() {
        runner.withPropertyValues("recipeforcode.openapi.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(OpenAPI.class));
    }

    @Test
    void doesNotCreateBeanWhenOpenApiClassMissing() {
        runner.withClassLoader(new FilteredClassLoader(OpenAPI.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(OpenAPI.class));
    }
}
