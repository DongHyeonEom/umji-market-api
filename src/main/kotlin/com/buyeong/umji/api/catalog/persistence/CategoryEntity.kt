package com.buyeong.umji.api.catalog.persistence

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainSoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "category")
class CategoryEntity : DomainSoftDeletableEntity() {
    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var path: String

    @Column(nullable = false)
    var depth: Int = 0

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "display_status", nullable = false)
    lateinit var displayStatus: String
}
