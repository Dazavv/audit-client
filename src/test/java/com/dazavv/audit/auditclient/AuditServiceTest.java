package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.audit.model.AuditEvent;
import com.dazavv.audit.auditclient.audit.model.AuditLogState;
import com.dazavv.audit.auditclient.audit.service.AuditService;
import com.dazavv.audit.auditclient.audit.service.KafkaAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

public class AuditServiceTest {
    @Test
    void testSendEventDelegatesToKafka() {
        KafkaAuditService kafkaService = Mockito.mock(KafkaAuditService.class);
        AuditService auditService = new AuditService(kafkaService);

        AuditEvent event = new AuditEvent();
        event.setAction("TEST");
        event.setClassName("TestClass");
        event.setMethodName("testMethod");
        event.setStatus(AuditLogState.SUCCESS);
        event.setTimestamp(Instant.now());

        auditService.sendEvent(event);

        Mockito.verify(kafkaService).sendAuditEvent(event);
    }
}
