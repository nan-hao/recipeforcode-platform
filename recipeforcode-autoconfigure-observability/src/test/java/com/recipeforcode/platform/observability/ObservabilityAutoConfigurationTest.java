package com.recipeforcode.platform.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void createsCustomizerAndAddsServiceTagWhenConfigured() {
        runner
                .withPropertyValues("recipeforcode.observability.service=order-service")
                .run(ctx -> {
                    assertThat(ctx).hasBean("observabilityCommonTagsCustomizer");

                    var customizer = ctx.getBean("observabilityCommonTagsCustomizer",
                            org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer.class);

                    MeterRegistry registry = new SimpleMeterRegistry();
                    // apply our customizer
                    customizer.customize(registry);

                    // create a meter and check tags on its ID
                    Counter c = Counter.builder("test.counter").register(registry);
                    assertThat(c.getId().getTags())
                            .anySatisfy(tag -> {
                                assertThat(tag.getKey()).isEqualTo("service");
                                assertThat(tag.getValue()).isEqualTo("order-service");
                            });
                });
    }

    @Test
    void doesNotAddServiceTagWhenBlank() {
        runner
                .withPropertyValues("recipeforcode.observability.service=") // blank on purpose
                .run(ctx -> {
                    assertThat(ctx).hasBean("observabilityCommonTagsCustomizer");

                    var customizer = ctx.getBean("observabilityCommonTagsCustomizer",
                            org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer.class);

                    MeterRegistry registry = new SimpleMeterRegistry();
                    customizer.customize(registry);

                    Counter c = Counter.builder("test.counter").register(registry);
                    assertThat(c.getId().getTags())
                            .noneMatch(t -> t.getKey().equals("service"));
                });
    }

    @Test
    void backsOffWhenUserProvidesCustomizerBeanWithSameName() {
        runner
                .withUserConfiguration(UserProvidedCustomizerConfig.class)
                .run(ctx -> {
                    // Auto-config backs off in favor of the user bean with the same name
                    assertThat(ctx).hasBean("observabilityCommonTagsCustomizer");
                    @SuppressWarnings("unchecked")
                    var customizer = (org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer<MeterRegistry>)
                            ctx.getBean("observabilityCommonTagsCustomizer");

                    MeterRegistry registry = new SimpleMeterRegistry();
                    customizer.customize(registry);
                    Counter c = Counter.builder("test.counter").register(registry);
                    assertThat(c.getId().getTags())
                            .anySatisfy(tag -> {
                                assertThat(tag.getKey()).isEqualTo("owner");
                                assertThat(tag.getValue()).isEqualTo("user");
                            });
                });
    }

    @Test
    void doesNotActivateWhenMicrometerClassesMissing() {
        runner
                .withClassLoader(new FilteredClassLoader(
                        io.micrometer.core.instrument.MeterRegistry.class,
                        org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean("observabilityCommonTagsCustomizer"));
    }

    @org.springframework.context.annotation.Configuration
    static class UserProvidedCustomizerConfig {
        @org.springframework.context.annotation.Bean(name = "observabilityCommonTagsCustomizer")
        org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer<MeterRegistry>
        userObservabilityCommonTagsCustomizer() {
            return registry -> registry.config().commonTags("owner", "user");
        }
    }
}
