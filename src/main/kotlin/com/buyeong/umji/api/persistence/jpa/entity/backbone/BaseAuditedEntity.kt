package com.buyeong.umji.api.persistence.jpa.entity.backbone

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 감사(Audit) 필드를 포함하는 기본 엔티티.
 *
 * JPA Auditing을 통해 생성/수정 시간과 사용자 정보가 자동으로 설정됩니다.
 *
 * @see AuditingEntityListener
 * @see com.buyeong.umji.api.config.persistence.AuditConfiguration
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAuditedEntity : BaseEntity() {
    @CreatedBy
    @Column(name = "created_by", updatable = false, nullable = false)
    var createdBy: Int = 0

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: LocalDateTime? = null

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    var updatedBy: Int = 0

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
}