package com.dazavv.audit.auditclient.service;

import com.dazavv.audit.auditclient.model.AuditEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuditService {
    private final AuditSender sender;

    public void sendEvent(AuditEvent auditEvent) {
        validateEvent(auditEvent);
        sender.send(auditEvent);
    }

    private void validateEvent(AuditEvent event) {
        if (event.getAction() == null || event.getAction().isBlank()) {
            throw new IllegalArgumentException("AuditEvent action cannot be null or empty");
        }
        if (event.getClassName() == null || event.getMethodName() == null) {
            throw new IllegalArgumentException("AuditEvent className and methodName cannot be null");
        }
        if (event.getStatus() == null) {
            throw new IllegalArgumentException("AuditEvent status cannot be null");
        }
        if (event.getRequestId() == null) {
            event.setRequestId("unknown");
        }
    }
}
