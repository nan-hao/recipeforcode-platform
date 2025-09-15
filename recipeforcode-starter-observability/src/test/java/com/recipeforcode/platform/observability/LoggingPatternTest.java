package com.recipeforcode.platform.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@SpringBootTest(classes = LoggingPatternTest.TestConfig.class, properties = "spring.application.name=test-app")
@ExtendWith(OutputCaptureExtension.class)
class LoggingPatternTest {
    private static final Logger log = LoggerFactory.getLogger(LoggingPatternTest.class);

    @Test
    void logsContainRequestIdFromMdc(CapturedOutput output) {
        MDC.put("requestId", "test-req-123");
        try {
            log.info("hello");
        } finally {
            MDC.clear();
        }
        // Expect our default JSON logging to include MDC and app
        String out = output.getOut();
        assert out.contains("\"requestId\":\"test-req-123\"");
        assert out.contains("\"app\":\"application\"");
    }

    @SpringBootConfiguration
    static class TestConfig { }
}
