package com.buyeong.umji.api.persistence.jpa.account.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "organization_address",
    uniqueConstraints = [UniqueConstraint(name = "UQ_organization_address_public_id", columnNames = ["public_id"])],
)
class OrganizationAddressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    var publicId: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    lateinit var organization: OrganizationEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_account_id", nullable = false)
    lateinit var createdByAccount: AccountEntity

    @Column(name = "recipient_name", nullable = false, length = 100)
    lateinit var recipientName: String

    @Column(name = "recipient_phone", nullable = false, length = 30)
    lateinit var recipientPhone: String

    @Column(name = "postal_code", nullable = false, length = 20)
    lateinit var postalCode: String

    @Column(name = "address1", nullable = false, length = 255)
    lateinit var address1: String

    @Column(name = "address2", length = 255)
    var address2: String? = null

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    fun assignPublicId() {
        if (publicId == null) publicId = UUID.randomUUID()
    }
}
