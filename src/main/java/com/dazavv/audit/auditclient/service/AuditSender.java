package com.dazavv.audit.auditclient.service;

import com.dazavv.audit.auditclient.model.AuditEvent;

public interface AuditSender {
    void send(AuditEvent event);
}
