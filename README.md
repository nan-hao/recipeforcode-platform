# RecipeForCode Platform

[![Release](https://img.shields.io/github/v/release/nan-hao/recipeforcode-platform?display_name=tag&sort=semver)](https://github.com/nan-hao/recipeforcode-platform/releases)
[![Java Build](https://github.com/nan-hao/recipeforcode-platform/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/nan-hao/recipeforcode-platform/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-24-007396?logo=java)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=nan-hao_recipeforcode-platform&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=nan-hao_recipeforcode-platform)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=nan-hao_recipeforcode-platform&metric=coverage)](https://sonarcloud.io/summary/new_code?id=nan-hao_recipeforcode-platform)

Multi-module Maven project providing a company BOM, a parent POM with standardized plugins (including Failsafe), and focused Spring Boot starters with auto-configuration for observability, resilience, and OpenAPI.

## Modules
- recipeforcode-platform-bom: Company BOM (`dependencyManagement`) importing Spring Boot and Testcontainers BOMs and pinning key libraries.
- recipeforcode-parent: Parent POM with `pluginManagement`, Enforcer, Surefire/Failsafe, reproducible builds, and BOM import.
- recipeforcode-autoconfigure-observability / recipeforcode-starter-observability
- recipeforcode-autoconfigure-resilience / recipeforcode-starter-resilience
- recipeforcode-autoconfigure-openapi / recipeforcode-starter-openapi

## Build
- CI-friendly versioning is used. Either:
  - Use the default from `.mvn/maven.config` (already sets `-Drevision=0.1.0-SNAPSHOT`), or
  - Pass it explicitly: `-Drevision=<version>`.
- Build all:
  - `mvn -T1C -DskipTests package` (uses `.mvn/maven.config`)
  - or `mvn -Drevision=0.1.0-SNAPSHOT -T1C -DskipTests package`
- Run unit tests: `mvn test`
- Run ITs (Failsafe): `mvn verify`

## Java, Spring Boot
- Group ID: `com.recipeforcode`
- Java: 24
- Spring Boot: 3.5.5

## Using in a new service
1. Create a new Spring Boot module with parent:
   ```xml
   <parent>
     <groupId>com.recipeforcode</groupId>
     <artifactId>recipeforcode-parent</artifactId>
     <version>${revision}</version>
     <relativePath>../recipeforcode-parent/pom.xml</relativePath>
   </parent>
   ```
2. Add starters as needed:
   ```xml
   <dependencies>
     <dependency>
       <groupId>com.recipeforcode</groupId>
       <artifactId>recipeforcode-starter-observability</artifactId>
     </dependency>
     <dependency>
       <groupId>com.recipeforcode</groupId>
       <artifactId>recipeforcode-starter-resilience</artifactId>
     </dependency>
     <dependency>
       <groupId>com.recipeforcode</groupId>
       <artifactId>recipeforcode-starter-openapi</artifactId>
     </dependency>
   </dependencies>
   ```
3. Configure properties as needed (examples):
   - `recipeforcode.observability.service=my-service`
   - `management.endpoints.web.exposure.include=health,info,prometheus`
   - `recipeforcode.openapi.title=My Service API`

## Observability usage
- Add starter: `com.recipeforcode:recipeforcode-starter-observability`
- What it does by default:
  - Adds Prometheus registry and Actuator endpoints (via Boot starter + registry dependency).
  - Auto-configures a `MeterRegistryCustomizer` named `observabilityCommonTagsCustomizer` that adds common tags:
    - `service` from `recipeforcode.observability.service` (if set)
    - `application` from `spring.application.name` (if set)
    - `environment` from `recipeforcode.observability.environment` (if set)
    - `version` from `BuildProperties` (if available)
  - Optional HTTP metrics tuning and logging context (MDC) features.

### Properties
- Core tags
  - `recipeforcode.observability.service` = logical service name
  - `recipeforcode.observability.environment` = env name (e.g., prod, staging)

- HTTP metrics percentiles (optional)
  - `recipeforcode.observability.http-percentiles` = list of percentiles to publish for `http.server.requests`
  - Example (YAML):
    ```yaml
    recipeforcode:
      observability:
        http-percentiles: [0.5, 0.9, 0.95, 0.99]
    ```

- MDC request correlation (optional, defaults on)
  - `recipeforcode.observability.add-request-id-mdc` = true|false (default true)
    - Adds `requestId` (from `X-Request-Id` header or a generated UUID) and `service` to MDC per request.
  - `recipeforcode.observability.mdc-headers` = list of header names to copy into MDC as `hdr.<header>`
  - Example logback pattern to include MDC: `%X{requestId} %X{service} %X{hdr.user-id}`

- Prometheus endpoint
  - Expose in your service: `management.endpoints.web.exposure.include=health,info,prometheus`
  - Scrape at `/actuator/prometheus`

- Overriding the default customizer
  - Define your own bean named `observabilityCommonTagsCustomizer` to replace the platform’s one.
  - The observability starter includes a default `logback-spring.xml` that outputs JSON via LogstashEncoder and includes MDC (e.g., `requestId`) and `app` from `spring.application.name`. Services can override by adding their own `logback-spring.xml`.

## Resilience usage
- Add starter: `com.recipeforcode:recipeforcode-starter-resilience`
- Defaults provided via customizers for the `default` instances:
  - CircuitBreaker: `slidingWindowSize=50`, `failureRateThreshold=50%`, `waitDurationInOpenState=5s`.
  - Retry: `maxAttempts=3`, `waitDuration=200ms`.
- Micrometer metrics integrate automatically if Micrometer is present (e.g., via observability starter).
- Override defaults by providing your own beans:
  ```java
  @Bean
  CircuitBreakerConfigCustomizer myCbDefaults() {
    return CircuitBreakerConfigCustomizer.of("default", b -> b.failureRateThreshold(25f));
  }

  @Bean
  RetryConfigCustomizer myRetryDefaults() {
    return RetryConfigCustomizer.of("default", b -> b.maxAttempts(5));
  }
  ```

## OpenAPI defaults
- Starter wiring forwards a single toggle to Springdoc:
  - `springdoc.api-docs.enabled=${recipeforcode.openapi.enabled:true}`
  - `springdoc.swagger-ui.enabled=${recipeforcode.openapi.enabled:true}`
- Location: `recipeforcode-starter-openapi/src/main/resources/application.yaml` in the starter.
- Behavior:
  - Enabled by default (true).
  - Set `recipeforcode.openapi.enabled=false` to disable both at once.
  - You can still override each Springdoc flag explicitly in your service if desired.

OpenAPI properties provided by auto-config (overridable):
- `recipeforcode.openapi.title` (default: `API`)
- `recipeforcode.openapi.description` (default: `API description`)
- `recipeforcode.openapi.version` (default: `1.0.0`)
- `recipeforcode.openapi.contact-name|contact-email|contact-url`
- `recipeforcode.openapi.license-name|license-url`

## Release and version alignment
- All modules share the same version managed at the root/parent.
- The platform BOM pins external library versions; services and starters rely on the BOM with no explicit versions.
- Update BOM versions first, then release a new aligned version across all modules.
