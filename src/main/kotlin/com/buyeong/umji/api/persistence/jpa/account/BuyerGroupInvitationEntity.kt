package com.buyeong.umji.api.persistence.jpa.account

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
    name = "buyer_group_invitation",
    uniqueConstraints = [UniqueConstraint(name = "UQ_buyer_group_invitation_public_id", columnNames = ["public_id"])],
)
class BuyerGroupInvitationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    var publicId: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_group_id", nullable = false)
    lateinit var buyerGroup: BuyerGroupEntity

    @Column(name = "phone_normalized", nullable = false, length = 30)
    lateinit var phoneNormalized: String

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_account_id", nullable = false)
    lateinit var invitedBy: AccountEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    var targetAccount: AccountEntity? = null

    @Column(nullable = false, length = 30)
    lateinit var status: String

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "responded_at")
    var respondedAt: Instant? = null

    @PrePersist
    fun assignPublicId() {
        if (publicId == null) publicId = UUID.randomUUID()
    }
}