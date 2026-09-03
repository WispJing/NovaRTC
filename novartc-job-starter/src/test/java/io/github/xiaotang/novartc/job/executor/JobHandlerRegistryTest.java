package io.github.xiaotang.novartc.job.executor;

import io.github.xiaotang.novartc.common.job.JobHandler;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobHandlerRegistryTest {
    @Test
    void onlyExplicitlyAnnotatedMethodsAreRegistered() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestHandler.class)) {
            JobHandlerRegistry registry = new JobHandlerRegistry(context);
            assertNotNull(registry.require(TestHandler.class.getName(), "allowed"));
            assertThrows(IllegalArgumentException.class, () -> registry.require(TestHandler.class.getName(), "notAllowed"));
        }
    }

    @Component
    static class TestHandler {
        @JobHandler(jobName = "test")
        public void allowed() {
        }

        public void notAllowed() {
        }
    }
}
