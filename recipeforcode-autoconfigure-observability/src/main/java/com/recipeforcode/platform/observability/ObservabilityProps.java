package com.recipeforcode.platform.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("recipeforcode.observability")
public record ObservabilityProps(String service, String environment,
                                 List<String> httpPercentiles,
                                 Boolean httpServerHistogram,
                                 List<String> mdcHeaders,
                                 Boolean addRequestIdMdc) {}
