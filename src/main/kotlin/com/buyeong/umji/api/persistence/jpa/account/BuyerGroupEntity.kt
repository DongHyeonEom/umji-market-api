package com.buyeong.umji.api.persistence.jpa.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "buyer_group")
class BuyerGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false)
    var publicId: UUID? = null

    @Column(name = "group_type", nullable = false)
    lateinit var groupType: String

    @Column(name = "display_name", nullable = false)
    lateinit var displayName: String

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    fun assignPublicId() {
        if (publicId == null) publicId = UUID.randomUUID()
    }
}