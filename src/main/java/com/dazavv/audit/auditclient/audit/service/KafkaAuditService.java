package com.dazavv.audit.auditclient.audit.service;

import com.dazavv.audit.auditclient.audit.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class KafkaAuditService {
    @Value("${topic.audit}")
    private String sendClientTopic;

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public void sendAuditEvent(AuditEvent auditEvent) {
        log.info("Sending audit event: action={}, requestId={}", auditEvent.getAction(), auditEvent.getRequestId());
        kafkaTemplate.send(sendClientTopic, auditEvent);
    }
}
