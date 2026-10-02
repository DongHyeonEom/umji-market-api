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
    name = "buyer_group_join_request",
    uniqueConstraints = [UniqueConstraint(name = "UQ_buyer_group_join_request_public_id", columnNames = ["public_id"])],
)
class BuyerGroupJoinRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    var publicId: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_group_id", nullable = false)
    lateinit var buyerGroup: BuyerGroupEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @Column(nullable = false, length = 30)
    lateinit var status: String

    @Column(name = "requested_at", nullable = false, updatable = false)
    var requestedAt: Instant = Instant.now()

    @Column(name = "responded_at")
    var respondedAt: Instant? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_account_id")
    var respondedBy: AccountEntity? = null

    @PrePersist
    fun assignPublicId() {
        if (publicId == null) publicId = UUID.randomUUID()
    }
}