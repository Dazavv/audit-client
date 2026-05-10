package com.dazavv.audit.auditclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

//    @NotBlank
    private String topic = "audit-events";
    private boolean enabled = true;

}