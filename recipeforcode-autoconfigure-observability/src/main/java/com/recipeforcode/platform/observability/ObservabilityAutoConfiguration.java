package com.recipeforcode.platform.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProps.class)
@ConditionalOnClass({MeterRegistry.class, MeterRegistryCustomizer.class})
public class ObservabilityAutoConfiguration {

    @Bean(name = "observabilityCommonTagsCustomizer")
    @ConditionalOnMissingBean(name = "observabilityCommonTagsCustomizer")
    MeterRegistryCustomizer<MeterRegistry> observabilityCommonTagsCustomizer(ObservabilityProps p,
                                                      ObjectProvider<BuildProperties> bp,
                                                      Environment env) {
        return registry -> {
            var tags = new ArrayList<Tag>();
            if (hasText(p.service())) tags.add(Tag.of("service", p.service()));
            var app = env.getProperty("spring.application.name");
            if (hasText(app)) tags.add(Tag.of("application", app));
            var environment = env.getProperty("recipeforcode.observability.environment");
            if (hasText(environment)) tags.add(Tag.of("environment", environment));
            bp.ifAvailable(b -> tags.add(Tag.of("version", b.getVersion())));
            registry.config().commonTags(tags);
        };
    }

    @Bean
    @ConditionalOnProperty(prefix="recipeforcode.observability", name="http-percentiles")
    MeterFilter httpPercentiles(ObservabilityProps p) {
        double[] ps = p.httpPercentiles().stream().mapToDouble(Double::parseDouble).toArray();
        return new MeterFilter() {
            @Override public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig c) {
                if ("http.server.requests".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(ps)
                            .build().merge(c);
                }
                return c;
            }
        };
    }

    @Bean
    @ConditionalOnClass(MDC.class)
    @ConditionalOnProperty(prefix="recipeforcode.observability", name="add-request-id-mdc", havingValue="true", matchIfMissing = true)
    FilterRegistrationBean<OncePerRequestFilter> mdcFilter(ObservabilityProps p) {
        OncePerRequestFilter f = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                try {
                    String requestId = req.getHeader("X-Request-Id");
                    if (!hasText(requestId)) requestId = UUID.randomUUID().toString();
                    MDC.put("requestId", requestId);
                    if (hasText(p.service())) MDC.put("service", p.service());
                    // copy selected headers to MDC (e.g., user id, tenant)
                    if (p.mdcHeaders()!=null) {
                        for (var h: p.mdcHeaders()) {
                            var v = req.getHeader(h);
                            if (hasText(v)) MDC.put("hdr." + h.toLowerCase(), v);
                        }
                    }
                    chain.doFilter(req, res);
                } finally {
                    MDC.clear();
                }
            }
        };
        var reg = new FilterRegistrationBean<>(f);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }


    private static boolean hasText(String s){ return s!=null && !s.isBlank(); }
}
