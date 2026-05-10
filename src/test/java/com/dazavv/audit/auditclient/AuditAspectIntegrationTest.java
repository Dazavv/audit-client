package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.annotation.Audit;
import com.dazavv.audit.auditclient.aspect.AuditAspect;
import com.dazavv.audit.auditclient.config.AuditAutoConfiguration;
import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.model.AuditLogState;
import com.dazavv.audit.auditclient.service.AuditSender;
import com.dazavv.audit.auditclient.service.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AuditAspectIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.application.name=test-app",
        "topic.audit=audit-topic-test"
})
@DisplayName("AuditAspect — интеграционные тесты перехвата событий")
class AuditAspectIntegrationTest {

    @MockBean
    private AuditService auditService;

    @Autowired
    private AuditTargetService auditTargetService;

    // -----------------------------------------------------------------
    // Минимальный Spring-контекст для теста библиотеки.
    // Содержит: аспект, целевой сервис с @Audit-методами,
    // включает AspectJ auto-proxy.
    // -----------------------------------------------------------------
    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableAutoConfiguration(exclude = AuditAutoConfiguration.class)
    static class TestConfig {

        @Bean
        public AuditSender auditSender() {
            return Mockito.mock(AuditSender.class);
        }

        @Bean
        public AuditService auditService(AuditSender sender) {
            return new AuditService(sender);
        }

        @Bean
        public AuditAspect auditAspect(AuditService auditService) {
            return new AuditAspect(auditService);
        }

        @Bean
        public AuditTargetService auditTargetService() {
            return new AuditTargetService();
        }
    }

    // -----------------------------------------------------------------
    // Целевой сервис с аннотированными методами.
    // Spring создаст прокси, AuditAspect перехватит вызовы.
    // -----------------------------------------------------------------
    @Service
    static class AuditTargetService {

        @Audit(action = "TEST_ACTION", logArgs = false)
        public String successfulOperation(String input) {
            return "result-" + input;
        }

        @Audit(action = "FAILING_ACTION")
        public void failingOperation() {
            throw new RuntimeException("Simulated failure");
        }

        @Audit(action = "ARGS_ACTION", logArgs = true)
        public void operationWithArgs(String sensitiveData) {
            // нам нужны только аргументы в событии
        }
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void setupSecurityContext(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setupRequestContext(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void setupRequestContextWithForwardedFor(String forwardedIp) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", forwardedIp + ", 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    // ===================================================================

    @Test
    @DisplayName("Успешный вызов: событие создаётся со статусом SUCCESS")
    void successfulMethodCallCreatesSuccessEvent() {
        setupSecurityContext("alice");
        setupRequestContext("192.168.1.10");

        auditTargetService.successfulOperation("test-arg");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, times(1)).sendEvent(captor.capture());

        AuditEvent event = captor.getValue();
        assertThat(event.getStatus()).isEqualTo(AuditLogState.SUCCESS);
        assertThat(event.getAction()).isEqualTo("TEST_ACTION");
        assertThat(event.getUserId()).isEqualTo("alice");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(event.getMethodName()).isEqualTo("successfulOperation");
        assertThat(event.getClassName()).contains("AuditTargetService");
        assertThat(event.getDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getRequestId()).isNotBlank();
    }

    @Test
    @DisplayName("Исключение в методе: событие создаётся со статусом FAILED")
    void failedMethodCallCreatesFailedEvent() {
        setupSecurityContext("bob");
        setupRequestContext("10.0.0.5");

        assertThatThrownBy(() -> auditTargetService.failingOperation())
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, times(1)).sendEvent(captor.capture());

        AuditEvent event = captor.getValue();
        assertThat(event.getStatus()).isEqualTo(AuditLogState.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("Simulated failure");
        assertThat(event.getUserId()).isEqualTo("bob");
    }

    @Test
    @DisplayName("IP берётся из X-Forwarded-For при наличии заголовка")
    void ipExtractedFromForwardedForHeader() {
        setupSecurityContext("carol");
        setupRequestContextWithForwardedFor("203.0.113.42");

        auditTargetService.successfulOperation("x");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).sendEvent(captor.capture());

        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.42");
    }

    @Test
    @DisplayName("Без SecurityContext userId = 'anonymous'")
    void withoutSecurityContextUserIdIsAnonymous() {
        setupRequestContext("127.0.0.1");

        auditTargetService.successfulOperation("x");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).sendEvent(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("logArgs = true: аргументы включаются в событие")
    void logArgsIncludesMethodArguments() {
        setupSecurityContext("dave");
        setupRequestContext("127.0.0.1");

        auditTargetService.operationWithArgs("secret-value");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).sendEvent(captor.capture());

        Object[] args = captor.getValue().getArguments();
        assertThat(args).isNotEmpty();
        assertThat(args[0]).isEqualTo("secret-value");
    }

    @Test
    @DisplayName("logArgs = false: аргументы не включаются в событие")
    void logArgsFalseExcludesArguments() {
        setupSecurityContext("eve");
        setupRequestContext("127.0.0.1");

        auditTargetService.successfulOperation("should-not-appear");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).sendEvent(captor.capture());

        assertThat(captor.getValue().getArguments()).isEmpty();
    }
}