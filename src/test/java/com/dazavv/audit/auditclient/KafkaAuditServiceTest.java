package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.audit.model.AuditEvent;
import com.dazavv.audit.auditclient.audit.model.AuditLogState;
import com.dazavv.audit.auditclient.audit.service.KafkaAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

public class KafkaAuditServiceTest {
    @Test
    void testSendAuditEvent() {
        KafkaTemplate<String, AuditEvent> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        KafkaAuditService service = new KafkaAuditService(kafkaTemplate);

        ReflectionTestUtils.setField(service, "sendClientTopic", "audit-topic");

        AuditEvent event = new AuditEvent();
        event.setAction("TEST");
        event.setClassName("TestClass");
        event.setMethodName("testMethod");
        event.setStatus(AuditLogState.SUCCESS);
        event.setTimestamp(Instant.now());

        service.sendAuditEvent(event);

        Mockito.verify(kafkaTemplate).send(Mockito.anyString(), Mockito.eq(event));
    }
}
