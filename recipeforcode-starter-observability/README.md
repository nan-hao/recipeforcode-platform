# recipeforcode-starter-observability

Add observability defaults for services: Prometheus metrics, common meter tags, and optional MDC request correlation.

## Add dependency
```xml
<dependency>
  <groupId>com.recipeforcode</groupId>
  <artifactId>recipeforcode-starter-observability</artifactId>
</dependency>
```

## Core configuration
```yaml
spring:
  application:
    name: my-service

recipeforcode:
  observability:
    service: my-service
    environment: prod

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

## Optional features
- HTTP percentiles for `http.server.requests`:
  ```yaml
  recipeforcode:
    observability:
      http-percentiles: [0.5, 0.9, 0.95, 0.99]
  ```

- MDC request correlation (enabled by default):
  ```yaml
  recipeforcode:
    observability:
      add-request-id-mdc: true
      mdc-headers: [User-Id, Tenant-Id]
  ```
  Include MDC keys in logs, e.g. `%X{requestId} %X{service} %X{hdr.user-id}`.

## Overriding the default customizer
Provide a bean named `observabilityCommonTagsCustomizer` to take full control of common meter tags.

