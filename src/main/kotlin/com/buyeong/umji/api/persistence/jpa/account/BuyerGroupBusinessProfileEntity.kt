package com.buyeong.umji.api.persistence.jpa.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "buyer_group_business_profile")
class BuyerGroupBusinessProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_group_id", nullable = false, unique = true)
    lateinit var buyerGroup: BuyerGroupEntity

    @Column(name = "business_name", nullable = false)
    lateinit var businessName: String

    @Column(name = "business_registration_number")
    var businessRegistrationNumber: String? = null

    @Column(name = "representative_name")
    var representativeName: String? = null

    @Column(name = "business_phone")
    var businessPhone: String? = null

    @Column(name = "business_industry", length = 100)
    var businessIndustry: String? = null

    @Column(name = "business_item", length = 100)
    var businessItem: String? = null

    @Column(name = "tax_invoice_email", length = 255)
    var taxInvoiceEmail: String? = null

    @Column(name = "postal_code")
    var postalCode: String? = null

    @Column(name = "address1")
    var address1: String? = null

    @Column(name = "address2")
    var address2: String? = null

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun touchUpdatedAt() {
        updatedAt = Instant.now()
    }
}
