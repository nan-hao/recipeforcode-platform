package com.recipeforcode.platform.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingPatternTest {

    @Test
    void shouldContainRequestIdFromMdc() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingPatternTest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        MDC.put("requestId", "test-req-123");
        try {
            logger.info("hello");
        } finally {
            MDC.clear();
        }

        assertThat(appender.list).isNotEmpty();
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "test-req-123");

        logger.detachAppender(appender);
        appender.stop();
    }
}
