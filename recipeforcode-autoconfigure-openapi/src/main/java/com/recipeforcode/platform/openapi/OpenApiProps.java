package com.recipeforcode.platform.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties("recipeforcode.openapi")
public record OpenApiProps(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("Recipeforcode API") String title,
        @DefaultValue("API documentation") String description,
        @DefaultValue("v1") String version,
        String termsOfService,
        String contactName,
        String contactEmail,
        String contactUrl,
        String licenseName,
        String licenseUrl,
        @DefaultValue("") List<String> serverUrls,
        @DefaultValue("public") String groupName,
        @DefaultValue("") List<String> packagesToScan,
        @DefaultValue("false") Boolean includeActuator,
        @DefaultValue("false") Boolean securityEnabled,
        @DefaultValue("bearer-jwt") String securitySchemeName
) {}