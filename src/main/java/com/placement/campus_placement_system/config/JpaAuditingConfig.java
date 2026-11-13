package com.placement.campus_placement_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing so entity createdAt and updatedAt fields are maintained automatically.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
