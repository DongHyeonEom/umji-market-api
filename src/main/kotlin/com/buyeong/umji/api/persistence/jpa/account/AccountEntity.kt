package com.buyeong.umji.api.persistence.jpa.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "account")
class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false)
    var publicId: UUID? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column
    var phone: String? = null

    @Column(name = "phone_normalized")
    var phoneNormalized: String? = null

    @Column
    var email: String? = null

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "token_version", nullable = false)
    var tokenVersion: Long = 0

    @Column(name = "last_login_at")
    var lastLoginAt: java.time.Instant? = null

    @PrePersist
    fun assignPublicId() {
        if (publicId == null) publicId = UUID.randomUUID()
    }
}
