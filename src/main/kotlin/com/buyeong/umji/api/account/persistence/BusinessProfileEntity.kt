package com.buyeong.umji.api.account.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "business_profile")
class BusinessProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @Column(name = "business_name", nullable = false)
    lateinit var businessName: String

    @Column(name = "business_registration_number")
    var businessRegistrationNumber: String? = null

    @Column(name = "representative_name")
    var representativeName: String? = null

    @Column(name = "business_phone")
    var businessPhone: String? = null

    @Column(name = "postal_code")
    var postalCode: String? = null

    @Column(name = "address1")
    var address1: String? = null

    @Column(name = "address2")
    var address2: String? = null

    @Column(nullable = false)
    lateinit var status: String
}
