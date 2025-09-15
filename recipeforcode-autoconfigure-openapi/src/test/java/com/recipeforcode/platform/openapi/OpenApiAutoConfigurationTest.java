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
            .withUserConfiguration()
            .withPropertyValues("recipeforcode.openapi.enabled=true");

    @Test
    void shouldCreateOpenApiBeanWhenEnabled() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(OpenAPI.class);
            var oas = ctx.getBean(OpenAPI.class);
            assertThat(oas.getInfo().getTitle()).isNotNull(); // defaults applied by props
        });
    }

    @Test
    void shouldNotCreateBeanWhenDisabled() {
        runner.withPropertyValues("recipeforcode.openapi.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(OpenAPI.class));
    }

    @Test
    void shouldNotCreateBeanWhenOpenApiClassMissing() {
        runner.withClassLoader(new FilteredClassLoader(OpenAPI.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(OpenAPI.class));
    }

    @Test
    void shouldApplyMetadataProperties() {
        runner.withPropertyValues(
                "recipeforcode.openapi.title=My API",
                "recipeforcode.openapi.description=Desc",
                "recipeforcode.openapi.version=9.9.9",
                "recipeforcode.openapi.contact-name=Team",
                "recipeforcode.openapi.contact-email=team@example.com",
                "recipeforcode.openapi.contact-url=https://example.com",
                "recipeforcode.openapi.license-name=Apache-2.0",
                "recipeforcode.openapi.license-url=https://www.apache.org/licenses/LICENSE-2.0"
        ).run(ctx -> {
            var oas = ctx.getBean(OpenAPI.class);
            var info = oas.getInfo();
            assertThat(info.getTitle()).isEqualTo("My API");
            assertThat(info.getDescription()).isEqualTo("Desc");
            assertThat(info.getVersion()).isEqualTo("9.9.9");
            assertThat(info.getContact().getName()).isEqualTo("Team");
            assertThat(info.getContact().getEmail()).isEqualTo("team@example.com");
            assertThat(info.getContact().getUrl()).isEqualTo("https://example.com");
            assertThat(info.getLicense().getName()).isEqualTo("Apache-2.0");
            assertThat(info.getLicense().getUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
        });
    }
}
