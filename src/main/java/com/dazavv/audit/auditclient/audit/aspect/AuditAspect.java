package com.dazavv.audit.auditclient.audit.aspect;

import com.dazavv.audit.auditclient.audit.annotation.Audit;
import com.dazavv.audit.auditclient.audit.model.AuditEvent;
import com.dazavv.audit.auditclient.audit.model.AuditLogState;
import com.dazavv.audit.auditclient.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.dazavv.audit.auditclient.audit.model.AuditLogState.*;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;

    @Value("${spring.application.name}")
    private String serviceName;

    @Pointcut("@annotation(com.dazavv.audit.auditclient.audit.annotation.Audit)")
    public void auditMethod() {
    }

    @Around("auditMethod() && @annotation(audit)")
    public Object collectLogs(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();

        try {

            Object result = joinPoint.proceed();

            auditService.sendEvent(
                    buildEvent(joinPoint, audit, SUCCESS, null, start)
            );

            return result;

        } catch (Exception ex) {

            auditService.sendEvent(
                    buildEvent(joinPoint, audit, FAILED, ex.getMessage(), start)
            );

            throw ex;
        }
    }
    private AuditEvent buildEvent(ProceedingJoinPoint joinPoint,
                                  Audit audit,
                                  AuditLogState status,
                                  String errorMessage,
                                  long startTime) {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] argsToLog = audit.logArgs() ? joinPoint.getArgs() : new Object[0];

        long duration = System.currentTimeMillis() - startTime;

        AuditEvent event = new AuditEvent();
        event.setAction(audit.action());
        event.setDescription(audit.description());
        event.setService(serviceName);
        event.setClassName(className);
        event.setMethodName(methodName);
        event.setArguments(argsToLog);
        event.setStatus(status);
        event.setErrorMessage(errorMessage);

        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = java.util.UUID.randomUUID().toString();
        }
        event.setRequestId(requestId);
        event.setTimestamp(Instant.now());
        event.setDurationMs(duration);

        return event;
    }
}
