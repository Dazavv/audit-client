package com.dazavv.audit.auditclient.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String action;
    private String description;
    private String service;
    private String className;
    private String methodName;

    private String requestId;

    private Object[] arguments;

    private AuditLogState status;
    private String errorMessage;

    private Instant timestamp;
    private long durationMs;
}
