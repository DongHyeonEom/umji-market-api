package com.buyeong.umji.api.config.persistence

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * JPA Auditing 설정.
 *
 * @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy 어노테이션을 활성화합니다.
 */
@Configuration
@EnableJpaAuditing
class AuditConfiguration {
    @Bean
    fun auditorProvider(): AuditorAware<Long> = AuditorAware { java.util.Optional.empty() }
}