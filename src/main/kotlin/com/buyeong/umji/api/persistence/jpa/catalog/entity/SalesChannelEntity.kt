package com.buyeong.umji.api.persistence.jpa.catalog.entity

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "sales_channel")
class SalesChannelEntity : DomainPublicEntity() {
    @Column(name = "code", nullable = false, unique = true, length = 30)
    lateinit var code: String

    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String
}