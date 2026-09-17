package com.buyeong.umji.api.persistence.jpa.entity.backbone

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.util.UUID

@MappedSuperclass
abstract class DomainPublicEntity : DomainBaseEntity() {
    @Column(name = "public_id", nullable = false, updatable = false)
    var publicId: UUID? = null
        protected set

    @PrePersist
    protected fun assignPublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID()
        }
    }
}