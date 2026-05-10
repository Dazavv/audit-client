package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.aspect.AuditAspect;
import com.dazavv.audit.auditclient.config.AuditAutoConfiguration;
import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.service.AuditSender;
import com.dazavv.audit.auditclient.service.AuditService;
import com.dazavv.audit.auditclient.service.KafkaAuditSender;
import com.dazavv.audit.auditclient.service.NoOpAuditSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("AuditAutoConfiguration — корректность подключения стартера")
class AuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class))
            .withPropertyValues(
                    "spring.application.name=test-app",
                    "topic.audit=audit-events"
            );

    @Test
    @DisplayName("Без KafkaTemplate в контексте: используется NoOpAuditSender")
    void noKafkaTemplateProducesNoOpSender() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuditSender.class);
            assertThat(context.getBean(AuditSender.class)).isInstanceOf(NoOpAuditSender.class);

            assertThat(context).hasSingleBean(AuditService.class);
            assertThat(context).hasSingleBean(AuditAspect.class);
        });
    }

    @Test
    @DisplayName("При наличии KafkaTemplate: используется KafkaAuditSender")
    void kafkaTemplatePresentProducesKafkaSender() {
        contextRunner
                .withUserConfiguration(KafkaTemplateConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditSender.class);
                    assertThat(context.getBean(AuditSender.class))
                            .isInstanceOf(KafkaAuditSender.class);
                });
    }

    @Test
    @DisplayName("Все ключевые бины стартера присутствуют в контексте")
    void allEssentialBeansArePresent() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("auditService");
            assertThat(context).hasBean("auditAspect");
            assertThat(context).hasSingleBean(AuditSender.class);
        });
    }

    @Configuration
    static class KafkaTemplateConfig {
        @Bean
        @SuppressWarnings("unchecked")
        public KafkaTemplate<String, AuditEvent> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}