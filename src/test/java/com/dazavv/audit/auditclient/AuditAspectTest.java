package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.audit.annotation.Audit;
import com.dazavv.audit.auditclient.audit.aspect.AuditAspect;
import com.dazavv.audit.auditclient.audit.model.AuditEvent;
import com.dazavv.audit.auditclient.audit.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

public class AuditAspectTest {
    static class TestClass {
        @Audit(action = "TEST_ACTION", description = "Test method", logArgs = true)
        public String annotatedMethod(String arg) {
            return "ok";
        }
    }

    @Test
    void testCollectLogsSuccess() throws Throwable {
        AuditService auditService = Mockito.mock(AuditService.class);
        AuditAspect aspect = new AuditAspect(auditService);
        TestClass obj = new TestClass();
        Method method = TestClass.class.getMethod("annotatedMethod", String.class);

        Audit audit = method.getAnnotation(Audit.class);

        ProceedingJoinPoint joinPoint = new ProceedingJoinPointMock(obj, method, new Object[]{"arg1"});
        Object result = aspect.collectLogs(joinPoint, audit);

        assert result.equals("ok");
        Mockito.verify(auditService).sendEvent(Mockito.any(AuditEvent.class));
    }

    @Test
    void testCollectLogsFailure() throws Throwable {
        AuditService auditService = Mockito.mock(AuditService.class);
        AuditAspect aspect = new AuditAspect(auditService);

        class FailingClass {
            @Audit(action = "FAIL_ACTION")
            public void failMethod() {
                throw new RuntimeException("fail");
            }
        }

        Method method = FailingClass.class.getMethod("failMethod");
        Audit audit = method.getAnnotation(Audit.class);
        ProceedingJoinPoint joinPoint = new ProceedingJoinPointMock(new FailingClass(), method, new Object[]{});

        try {
            aspect.collectLogs(joinPoint, audit);
        } catch (RuntimeException e) {
            assert e.getMessage().equals("fail");
        }

        Mockito.verify(auditService).sendEvent(Mockito.any(AuditEvent.class));
    }
}
