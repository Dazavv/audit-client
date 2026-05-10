package com.dazavv.audit.auditclient.service;

import com.dazavv.audit.auditclient.model.AuditEvent;

public class NoOpAuditSender implements AuditSender {
    @Override
    public void send(AuditEvent event) {
    }
}