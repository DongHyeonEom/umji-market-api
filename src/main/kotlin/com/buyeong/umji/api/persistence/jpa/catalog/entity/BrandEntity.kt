package com.buyeong.umji.api.persistence.jpa.catalog.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainSoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class BrandEntity : DomainSoftDeletableEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    var organization: OrganizationEntity? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column(name = "display_status", nullable = false)
    lateinit var displayStatus: String
}