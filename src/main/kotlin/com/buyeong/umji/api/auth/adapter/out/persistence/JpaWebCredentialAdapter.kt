package com.buyeong.umji.api.auth.adapter.out.persistence

import com.buyeong.umji.api.auth.application.model.AccountRecord
import com.buyeong.umji.api.auth.application.model.WebAccountCredentials
import com.buyeong.umji.api.auth.application.port.out.WebCredentialPort
import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class JpaWebCredentialAdapter(
    private val accounts: AccountJpaEntityService,
    private val jdbc: JdbcTemplate,
    @Value("\${umji.auth.totp-encryption-key:}") private val encryptionKeyBase64: String,
) : WebCredentialPort {
    @Transactional(readOnly = true)
    override fun findByNormalizedPhone(phone: String): WebAccountCredentials? = accounts.findByPhoneNormalized(phone)?.toCredentials()

    @Transactional(readOnly = true)
    override fun findById(id: UUID): WebAccountCredentials? = accounts.findByPublicId(id)?.toCredentials()

    @Transactional
    override fun savePassword(id: UUID, passwordHash: String) {
        val entity = accounts.findByPublicId(id) ?: return
        entity.passwordHash = passwordHash
        entity.tokenVersion++
        accounts.save(entity)
        jdbc.update(
            "UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP(3) WHERE account_id = ? AND revoked_at IS NULL",
            requireNotNull(entity.id),
        )
    }

    @Transactional
    override fun saveTotpSecret(id: UUID, secret: String, enabled: Boolean) {
        val entity = accounts.findByPublicId(id) ?: return
        entity.adminTotpSecretEncrypted = encrypt(secret)
        entity.adminTotpEnabled = enabled
        if (enabled) {
            entity.tokenVersion++
            jdbc.update(
                "UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP(3) WHERE account_id = ? AND revoked_at IS NULL",
                requireNotNull(entity.id),
            )
        }
        accounts.save(entity)
    }

    @Transactional
    override fun resetTotp(id: UUID) {
        val entity = accounts.findByPublicId(id) ?: return
        entity.adminTotpSecretEncrypted = null
        entity.adminTotpEnabled = false
        entity.tokenVersion++
        accounts.save(entity)
        jdbc.update(
            "UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP(3) WHERE account_id = ? AND revoked_at IS NULL",
            requireNotNull(entity.id),
        )
    }

    private fun AccountEntity.toCredentials(): WebAccountCredentials {
        val internalId = requireNotNull(id)
        val permissions = jdbc.queryForList(
            """SELECT DISTINCT p.code FROM account_role ar
                JOIN role_permission rp ON rp.role_id = ar.role_id
                JOIN permission p ON p.id = rp.permission_id WHERE ar.account_id = ?
            """.trimIndent(),
            String::class.java,
            internalId,
        ).toSet()
        val roles = jdbc.queryForList(
            "SELECT r.code FROM account_role ar JOIN role r ON r.id = ar.role_id WHERE ar.account_id = ?",
            String::class.java,
            internalId,
        ).toSet()
        val account = AccountRecord(requireNotNull(publicId), name, status, tokenVersion, permissions)
        return WebAccountCredentials(account, passwordHash, roles, adminTotpSecretEncrypted?.let(::decrypt), adminTotpEnabled)
    }

    private fun encrypt(value: String): ByteArray {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey(), "AES"), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    }

    private fun decrypt(value: ByteArray): String {
        require(value.size > 12) { "Invalid encrypted TOTP secret" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey(), "AES"), GCMParameterSpec(128, value.copyOfRange(0, 12)))
        return cipher.doFinal(value.copyOfRange(12, value.size)).toString(Charsets.UTF_8)
    }

    private fun encryptionKey(): ByteArray = runCatching { Base64.getDecoder().decode(encryptionKeyBase64) }
        .getOrNull()?.takeIf { it.size == 32 }
        ?: error("umji.auth.totp-encryption-key must be a Base64 encoded 32-byte key")

    private companion object {
        val random = SecureRandom()
    }
}