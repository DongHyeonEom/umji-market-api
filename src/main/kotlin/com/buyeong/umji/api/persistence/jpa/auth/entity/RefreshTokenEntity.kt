package com.buyeong.umji.api.persistence.jpa.auth

import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
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
@Table(name = "refresh_token")
class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @Column(name = "token_hash", nullable = false, unique = true)
    lateinit var tokenHash: ByteArray

    @Column(name = "device_id")
    var deviceId: String? = null

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null

    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
