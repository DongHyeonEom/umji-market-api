package com.buyeong.umji.api.persistence.jpa.account.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "organization_capability",
    uniqueConstraints = [UniqueConstraint(name = "UQ_organization_capability_org_code", columnNames = ["organization_id", "capability_code"])],
)
class OrganizationCapabilityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    lateinit var organization: OrganizationEntity

    @Column(name = "capability_code", nullable = false, length = 30)
    lateinit var capabilityCode: String

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}