package com.recipeforcode.platform.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
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
    void shouldAddServiceTagWhenConfigured() {
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
    void shouldNotAddServiceTagWhenBlank() {
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
    void shouldConfigureHttpPercentilesFilter() {
        runner.withPropertyValues(
                "recipeforcode.observability.http-percentiles=0.5,0.9,0.99",
                "recipeforcode.observability.add-request-id-mdc=false" // avoid servlet requirement
        ).run(ctx -> {
            assertThat(ctx).hasSingleBean(org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer.class);
            var meterFilter = ctx.getBean(io.micrometer.core.instrument.config.MeterFilter.class);
            var id = new io.micrometer.core.instrument.Meter.Id(
                    "http.server.requests",
                    Tags.of("uri", "/test"),
                    null,
                    null,
                    io.micrometer.core.instrument.Meter.Type.TIMER
            );
            var cfg = meterFilter.configure(id, io.micrometer.core.instrument.distribution.DistributionStatisticConfig.NONE);
            assertThat(cfg.getPercentiles()).containsExactly(0.5, 0.9, 0.99);
        });
    }

    @Test
    void shouldIncludeCommonTagsApplicationAndEnvironment() {
        runner.withPropertyValues(
                "spring.application.name=my-app",
                "recipeforcode.observability.environment=prod",
                "recipeforcode.observability.service=svc"
        ).run(ctx -> {
            var customizer = ctx.getBean("observabilityCommonTagsCustomizer",
                    org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer.class);
            MeterRegistry registry = new SimpleMeterRegistry();
            customizer.customize(registry);
            var c = Counter.builder("demo").register(registry);
            assertThat(c.getId().getTags()).extracting("key").contains("service", "application", "environment");
        });
    }

    @Test
    void shouldBackOffWhenUserProvidesCustomizerBeanWithSameName() {
        runner
                .withUserConfiguration(UserProvidedCustomizerConfig.class)
                .run(ctx -> {
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
    void shouldNotActivateWhenMicrometerClassesMissing() {
        runner
                .withClassLoader(new FilteredClassLoader(
                        io.micrometer.core.instrument.MeterRegistry.class,
                        org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean("observabilityCommonTagsCustomizer"));
    }

    @Test
    void shouldPopulateMdcWithRequestIdServiceAndHeaders() {
        runner.withPropertyValues(
                "recipeforcode.observability.service=svc",
                "recipeforcode.observability.mdc-headers=User-Id, Tenant-Id",
                "recipeforcode.observability.add-request-id-mdc=true"
        ).run(ctx -> {
            var reg = ctx.getBean("mdcFilter", org.springframework.boot.web.servlet.FilterRegistrationBean.class);
            var filter = (org.springframework.web.filter.OncePerRequestFilter) reg.getFilter();

            var req = new org.springframework.mock.web.MockHttpServletRequest();
            var res = new org.springframework.mock.web.MockHttpServletResponse();
            req.addHeader("X-Request-Id", "abc-123");
            req.addHeader("User-Id", "u1");

            final boolean[] checked = {false};
            try {
                filter.doFilter(req, res, (request, response) -> {
                    // Validate MDC contents inside the filter chain
                    assertThat(org.slf4j.MDC.get("requestId")).isEqualTo("abc-123");
                    assertThat(org.slf4j.MDC.get("service")).isEqualTo("svc");
                    assertThat(org.slf4j.MDC.get("hdr.user-id")).isEqualTo("u1");
                    checked[0] = true;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            assertThat(checked[0]).isTrue();
            // After filter completes, MDC should be cleared
            assertThat(org.slf4j.MDC.get("requestId")).isNull();
            assertThat(org.slf4j.MDC.get("service")).isNull();
        });
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderMissing() {
        runner.withPropertyValues(
                "recipeforcode.observability.service=svc",
                "recipeforcode.observability.add-request-id-mdc=true"
        ).run(ctx -> {
            var reg = ctx.getBean("mdcFilter", org.springframework.boot.web.servlet.FilterRegistrationBean.class);
            var filter = (org.springframework.web.filter.OncePerRequestFilter) reg.getFilter();

            var req = new org.springframework.mock.web.MockHttpServletRequest();
            var res = new org.springframework.mock.web.MockHttpServletResponse();

            final String[] seenId = {null};
            filter.doFilter(req, res, (request, response) -> {
                var id = org.slf4j.MDC.get("requestId");
                seenId[0] = id;
                assertThat(id).isNotBlank();
                // looks like a UUID (basic regex to avoid strict dependency)
                assertThat(id).matches("[0-9a-fA-F\\-]{20,}");
            });

            // ensure cleared after chain
            assertThat(org.slf4j.MDC.get("requestId")).isNull();
            assertThat(seenId[0]).isNotBlank();
        });
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
