package com.dazavv.audit.auditclient.service;

import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.config.AuditProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class KafkaAuditSender implements AuditSender {

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final AuditProperties properties;

    @Override
    public void send(AuditEvent event) {
        kafkaTemplate.send(properties.getTopic(), event)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        log.error(
                                "Failed to send audit event: action={}, requestId={}, topic={}",
                                event.getAction(),
                                event.getRequestId(),
                                properties.getTopic(),
                                ex
                        );
                        return;
                    }

                    log.debug(
                            "Audit event sent successfully: action={}, requestId={}, partition={}, offset={}",
                            event.getAction(),
                            event.getRequestId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }

}
