package com.buyeong.umji.api.persistence.jpa.entity.backbone

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class DomainSoftDeletableEntity : DomainPublicEntity() {
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        protected set

    @Column(name = "deleted_by")
    var deletedBy: Long? = null
        protected set

    fun markDeleted(
        deletedAt: Instant,
        deletedBy: Long?,
    ) {
        this.deletedAt = deletedAt
        this.deletedBy = deletedBy
    }

    fun restore() {
        deletedAt = null
        deletedBy = null
    }
}