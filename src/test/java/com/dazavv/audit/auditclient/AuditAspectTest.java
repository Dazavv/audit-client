package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.annotation.Audit;
import com.dazavv.audit.auditclient.aspect.AuditAspect;
import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditAspectTest {

    static class TestClass {

        @Audit(
                action = "TEST_ACTION",
                description = "Test method",
                logArgs = true
        )
        public String annotatedMethod(String arg) {
            return "ok";
        }
    }

    @Test
    void testCollectLogsSuccess() throws Throwable {

        // Arrange
        AuditService auditService = mock(AuditService.class);
        AuditAspect aspect = new AuditAspect(auditService);

        Method method = TestClass.class
                .getMethod("annotatedMethod", String.class);

        Audit audit = method.getAnnotation(Audit.class);

        Signature signature = mock(Signature.class);

        when(signature.getName())
                .thenReturn(method.getName());

        when(signature.getDeclaringTypeName())
                .thenReturn(method.getDeclaringClass().getName());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getArgs())
                .thenReturn(new Object[]{"arg1"});
        when(joinPoint.getSignature())
                .thenReturn(signature);

        // Act
        Object result = aspect.collectLogs(joinPoint, audit);

        // Assert
        assertThat(result).isEqualTo("ok");

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(auditService).sendEvent(captor.capture());

        AuditEvent event = captor.getValue();

        assertThat(event.getAction())
                .isEqualTo("TEST_ACTION");

        assertThat(event.getDescription())
                .isEqualTo("Test method");

        assertThat(event.getMethodName())
                .isEqualTo("annotatedMethod");

        assertThat(event.getStatus().name())
                .isEqualTo("SUCCESS");

        assertThat(event.getArguments())
                .containsExactly("arg1");
    }

    @Test
    void testCollectLogsFailure() throws Throwable {

        // Arrange
        AuditService auditService = mock(AuditService.class);
        AuditAspect aspect = new AuditAspect(auditService);

        class FailingClass {

            @Audit(action = "FAIL_ACTION")
            public void failMethod() {
                throw new RuntimeException("fail");
            }
        }

        Method method =
                FailingClass.class.getMethod("failMethod");

        Audit audit = method.getAnnotation(Audit.class);

        Signature signature = mock(Signature.class);

        when(signature.getName())
                .thenReturn(method.getName());

        when(signature.getDeclaringTypeName())
                .thenReturn(method.getDeclaringClass().getName());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.proceed())
                .thenThrow(new RuntimeException("fail"));

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{});

        when(joinPoint.getSignature())
                .thenReturn(signature);

        // Act + Assert
        try {
            aspect.collectLogs(joinPoint, audit);
        } catch (RuntimeException e) {

            assertThat(e.getMessage())
                    .isEqualTo("fail");
        }

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(auditService).sendEvent(captor.capture());

        AuditEvent event = captor.getValue();

        assertThat(event.getStatus().name())
                .isEqualTo("FAILED");

        assertThat(event.getErrorMessage())
                .isEqualTo("fail");
    }
}