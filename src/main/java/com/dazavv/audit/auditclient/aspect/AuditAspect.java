package com.dazavv.audit.auditclient.aspect;

import com.dazavv.audit.auditclient.annotation.Audit;
import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.model.AuditLogState;
import com.dazavv.audit.auditclient.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

import static com.dazavv.audit.auditclient.model.AuditLogState.*;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditService auditService;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Pointcut("@annotation(com.dazavv.audit.auditclient.annotation.Audit)")
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
    // AuditAspect.java — метод buildEvent, дополненная версия

    private AuditEvent buildEvent(ProceedingJoinPoint joinPoint,
                                  Audit audit,
                                  AuditLogState status,
                                  String errorMessage,
                                  long startTime) {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] argsToLog = audit.logArgs() ? joinPoint.getArgs() : new Object[0];
        long duration = System.currentTimeMillis() - startTime;

        // userId из Spring Security
        String userId = extractUserId();

        // IP из текущего HTTP-запроса
        String ipAddress = extractIpAddress();

        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        AuditEvent event = new AuditEvent();
        event.setAction(audit.action());
        event.setDescription(audit.description());
        event.setService(serviceName);
        event.setClassName(className);
        event.setMethodName(methodName);
        event.setArguments(argsToLog);
        event.setStatus(status);
        event.setErrorMessage(errorMessage);
        event.setRequestId(requestId);
        event.setUserId(userId);
        event.setIpAddress(ipAddress);
        event.setTimestamp(Instant.now());
        event.setDurationMs(duration);

        return event;
    }

    private String extractUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from SecurityContext", e);
        }
        return "anonymous";
    }

    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Could not extract IP address from request context", e);
        }
        return "unknown";
    }
}
