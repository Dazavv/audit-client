package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.model.AuditLogState;
import com.dazavv.audit.auditclient.service.AuditSender;
import com.dazavv.audit.auditclient.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("AuditService — валидация и делегирование отправителю")
class AuditServiceTest {

    private AuditSender sender;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        sender = mock(AuditSender.class);
        auditService = new AuditService(sender);
    }

    private AuditEvent validEvent() {
        AuditEvent e = new AuditEvent();
        e.setAction("LOGIN");
        e.setClassName("AuthService");
        e.setMethodName("login");
        e.setStatus(AuditLogState.SUCCESS);
        e.setTimestamp(Instant.now());
        return e;
    }

    @Test
    @DisplayName("Валидное событие делегируется отправителю")
    void validEventIsDelegated() {
        AuditEvent event = validEvent();
        auditService.sendEvent(event);
        verify(sender, times(1)).send(event);
    }

    @Test
    @DisplayName("Пустой action отбрасывается с IllegalArgumentException")
    void blankActionIsRejected() {
        AuditEvent event = validEvent();
        event.setAction("   ");

        assertThatThrownBy(() -> auditService.sendEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");

        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("Null className отбрасывается")
    void nullClassNameIsRejected() {
        AuditEvent event = validEvent();
        event.setClassName(null);

        assertThatThrownBy(() -> auditService.sendEvent(event))
                .isInstanceOf(IllegalArgumentException.class);

        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("Null status отбрасывается")
    void nullStatusIsRejected() {
        AuditEvent event = validEvent();
        event.setStatus(null);

        assertThatThrownBy(() -> auditService.sendEvent(event))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null requestId заменяется на 'unknown'")
    void nullRequestIdIsDefaulted() {
        AuditEvent event = validEvent();
        event.setRequestId(null);

        auditService.sendEvent(event);

        assertThat(event.getRequestId()).isEqualTo("unknown");
        verify(sender).send(event);
    }
}