package com.buyeong.umji.api.persistence.jpa.account.entity

import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "brand_shipping_assignment")
class BrandShippingAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    lateinit var brand: BrandEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_account_id", nullable = false)
    lateinit var assignedBy: AccountEntity

    @Column(name = "assignment_type", nullable = false)
    lateinit var assignmentType: String

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
