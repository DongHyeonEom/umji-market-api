package com.buyeong.umji.api.config.persistence

import com.buyeong.umji.api.persistence.jpa.entity.backbone.SimpleAuditorAware
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
    /**
     * Auditor 제공자.
     *
     * createdBy, updatedBy 필드에 자동으로 값을 설정합니다.
     * 실제 프로젝트에서는 SecurityContext에서 사용자 ID를 가져오도록 구현해야 합니다.
     */
    @Bean
    fun auditorProvider(): AuditorAware<Int> = SimpleAuditorAware()
}