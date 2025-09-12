# recipeforcode-starter-resilience

Resilience4j starter that provides sensible defaults for circuit breakers and retries, with Spring Boot 3 integration.

## Add dependency
```xml
<dependency>
  <groupId>com.recipeforcode</groupId>
  <artifactId>recipeforcode-starter-resilience</artifactId>
</dependency>
```

## What you get
- Default customizers for the `default` instances:
  - CircuitBreaker: `slidingWindowSize=50`, `failureRateThreshold=50%`, `waitDurationInOpenState=5s`.
  - Retry: `maxAttempts=3`, `waitDuration=200ms`.
- Conditional activation: only when Resilience4j classes are on the classpath.
- Micrometer integration: automatically active if Micrometer is present (e.g., via `spring-boot-starter-actuator`).

## Override defaults
- Provide your own beans to override:
  ```java
  @Bean
  CircuitBreakerConfigCustomizer myCbDefaults() {
    return CircuitBreakerConfigCustomizer.of("default", builder -> builder.failureRateThreshold(25f));
  }

  @Bean
  RetryConfigCustomizer myRetryDefaults() {
    return RetryConfigCustomizer.of("default", builder -> builder.maxAttempts(5));
  }
  ```

## Notes
- You can still configure named instances via properties (`resilience4j.circuitbreaker.instances.*`, `resilience4j.retry.instances.*`); the customizers act as a baseline.
- For metrics, include the observability starter or at least actuator + a registry (e.g., Prometheus).

