package com.buyeong.umji.api.catalog.persistence

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainSoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class BrandEntity : DomainSoftDeletableEntity() {
    @Column(nullable = false)
    lateinit var name: String

    @Column(name = "display_status", nullable = false)
    lateinit var displayStatus: String
}
