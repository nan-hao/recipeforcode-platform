package com.recipeforcode.platform.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(OpenApiProps.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "recipeforcode.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiAutoConfiguration {

    /**
     * Provides a default OpenAPI bean for the organization.
     * - Respects values exactly as provided by users (incl. blanks).
     * - Is created only if:
     *     * springdoc is on the classpath,
     *     * web app is present,
     *     * recipeforcode.openapi.enabled=true (or missing),
     *     * and no other OpenAPI bean exists.
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI platformOpenApi(OpenApiProps props) {
        var info = new Info()
                .title(props.title())
                .description(props.description())
                .version(props.version());

        if (props.contactName() != null || props.contactEmail() != null || props.contactUrl() != null) {
            var contact = new Contact()
                    .name(props.contactName())
                    .email(props.contactEmail())
                    .url(props.contactUrl());
            info.setContact(contact);
        }

        if (props.licenseName() != null || props.licenseUrl() != null) {
            info.setLicense(new License()
                    .name(props.licenseName())
                    .url(props.licenseUrl()));
        }

        return new OpenAPI().info(info);
    }
}