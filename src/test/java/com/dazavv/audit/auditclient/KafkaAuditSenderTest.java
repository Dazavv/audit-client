package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.config.AuditProperties;
import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.model.AuditLogState;
import com.dazavv.audit.auditclient.service.KafkaAuditSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("KafkaAuditSender — отправка событий в Kafka")
class KafkaAuditSenderTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate =
            mock(KafkaTemplate.class);

    private final AuditProperties properties =
            mock(AuditProperties.class);

    private KafkaAuditSender sender;

    @BeforeEach
    void setUp() {
        when(properties.getTopic()).thenReturn("audit-events");

        sender = new KafkaAuditSender(kafkaTemplate, properties);

        when(kafkaTemplate.send(any(String.class), any(AuditEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("Событие отправляется в настроенный топик")
    void eventIsSentToConfiguredTopic() {

        AuditEvent event = new AuditEvent();
        event.setAction("TEST");
        event.setStatus(AuditLogState.SUCCESS);
        event.setTimestamp(Instant.now());
        event.setRequestId("req-1");

        sender.send(event);

        verify(kafkaTemplate, times(1))
                .send(eq("audit-events"), eq(event));
    }
}