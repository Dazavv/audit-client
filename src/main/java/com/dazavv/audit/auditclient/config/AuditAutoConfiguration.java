package com.dazavv.audit.auditclient.config;

import com.dazavv.audit.auditclient.aspect.AuditAspect;
import com.dazavv.audit.auditclient.model.AuditEvent;
import com.dazavv.audit.auditclient.service.AuditSender;
import com.dazavv.audit.auditclient.service.AuditService;
import com.dazavv.audit.auditclient.service.KafkaAuditSender;
import com.dazavv.audit.auditclient.service.NoOpAuditSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    public AuditSender auditSender() {
        return new NoOpAuditSender();
    }

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    public AuditSender kafkaAuditSender(
            KafkaTemplate<String, AuditEvent> kafkaTemplate,
            AuditProperties properties
    ) {
        return new KafkaAuditSender(kafkaTemplate, properties);
    }

    @Bean
    public AuditService auditService(AuditSender auditSender) {
        return new AuditService(auditSender);
    }

    @Bean
    public AuditAspect auditAspect(AuditService auditService) {
        return new AuditAspect(auditService);
    }
}