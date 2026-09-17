package com.buyeong.umji.api.persistence.jpa.entity.backbone

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseCreateAuditedEntity : BaseEntity() {
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
}