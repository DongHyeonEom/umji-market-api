package com.buyeong.umji.api.auth.adapter.out.persistence

import com.buyeong.umji.api.auth.application.model.AccountRecord
import com.buyeong.umji.api.auth.application.model.RefreshSessionRecord
import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.auth.application.port.out.RefreshSessionPort
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.auth.RefreshTokenEntity
import com.buyeong.umji.api.persistence.jpa.auth.RefreshTokenJpaEntityService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.nio.ByteBuffer
import java.util.UUID

@Component
class JpaAuthenticationStoreAdapter(
    private val accounts: AccountJpaEntityService,
    private val refreshTokens: RefreshTokenJpaEntityService,
    private val jdbc: JdbcTemplate,
) : AccountAuthenticationPort, RefreshSessionPort {
    @Transactional(readOnly = true)
    override fun findByPublicId(id: java.util.UUID): AccountRecord? = accounts.findByPublicId(id)?.toRecord()

    @Transactional(readOnly = true)
    override fun findByNormalizedPhone(phone: String): AccountRecord? = accounts.findByPhoneNormalized(phone)?.toRecord()

    @Transactional(readOnly = true)
    override fun isTokenCurrent(id: UUID, tokenVersion: Long): Boolean =
        jdbc.query(
            "SELECT 1 FROM account WHERE public_id = ? AND status = 'ACTIVE' AND token_version = ?",
            { result, _ -> result.getInt(1) == 1 },
            ByteBuffer.allocate(16).putLong(id.mostSignificantBits).putLong(id.leastSignificantBits).array(),
            tokenVersion,
        ).firstOrNull() == true

    @Transactional
    override fun recordLogin(accountId: java.util.UUID, at: Instant) {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        account.lastLoginAt = at
    }

    @Transactional
    override fun findLockedByHash(tokenHash: ByteArray): RefreshSessionRecord? =
        refreshTokens.findLockedByTokenHash(tokenHash)?.toRecord()

    @Transactional
    override fun save(session: RefreshSessionRecord) {
        val account = accounts.findByPublicId(session.account.id) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val entity = refreshTokens.findLockedByTokenHash(session.tokenHash) ?: RefreshTokenEntity()
        entity.account = account
        entity.tokenHash = session.tokenHash
        entity.deviceId = session.deviceId
        entity.revokedAt = session.revokedAt
        entity.lastUsedAt = session.lastUsedAt
        entity.expiresAt = session.expiresAt
        refreshTokens.save(entity)
    }

    private fun AccountEntity.toRecord(): AccountRecord {
        val accountPublicId = requireNotNull(publicId)
        val permissions = jdbc.queryForList(
            """SELECT DISTINCT p.code
                FROM account_role ar
                JOIN role_permission rp ON rp.role_id = ar.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE ar.account_id = ?""".trimIndent(),
            String::class.java,
            requireNotNull(id),
        ).toSet()
        return AccountRecord(accountPublicId, name, status, tokenVersion, permissions)
    }
    private fun RefreshTokenEntity.toRecord() = RefreshSessionRecord(
        tokenHash, account.toRecord(), deviceId, revokedAt, lastUsedAt, expiresAt,
    )
}
