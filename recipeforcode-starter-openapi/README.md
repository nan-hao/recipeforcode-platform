# recipeforcode-starter-openapi

Opinionated OpenAPI starter that provides sensible defaults and a single toggle to enable/disable Springdoc in one place.

## Add dependency
```xml
<dependency>
  <groupId>com.recipeforcode</groupId>
  <artifactId>recipeforcode-starter-openapi</artifactId>
</dependency>
```

## Quick start
- By default, OpenAPI is enabled. You can disable both API docs and Swagger UI with one property:
  ```yaml
  recipeforcode:
    openapi:
      enabled: false
  ```
- The starter ships an `application.yaml` that forwards a single toggle to Springdoc:
  - `springdoc.api-docs.enabled = ${recipeforcode.openapi.enabled:true}`
  - `springdoc.swagger-ui.enabled = ${recipeforcode.openapi.enabled:true}`

## Customize OpenAPI metadata
Provided by auto-config (overridable via properties):
```yaml
recipeforcode:
  openapi:
    title: My Service API
    description: API for My Service
    version: 1.2.3
    contact-name: Team Alpha
    contact-email: team@example.com
    contact-url: https://example.com/team
    license-name: Apache-2.0
    license-url: https://www.apache.org/licenses/LICENSE-2.0
```

## Override the OpenAPI bean
- If you define your own `@Bean OpenAPI`, the platform will back off and use yours.

