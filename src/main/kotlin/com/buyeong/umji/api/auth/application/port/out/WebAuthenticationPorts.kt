package com.buyeong.umji.api.auth.application.port.out

import com.buyeong.umji.api.auth.application.model.WebAccountCredentials
import java.util.UUID

interface WebCredentialPort {
    fun findByNormalizedPhone(phone: String): WebAccountCredentials?
    fun findById(id: UUID): WebAccountCredentials?
    fun savePassword(id: UUID, passwordHash: String)
    fun saveTotpSecret(id: UUID, secret: String, enabled: Boolean)
    fun resetTotp(id: UUID)
}

interface CredentialEncoderPort {
    fun encode(raw: String): String
    fun matches(raw: String, encoded: String): Boolean
}

interface TotpPort {
    fun newSecret(): String
    fun provisioningUri(accountName: String, secret: String): String
    fun verify(secret: String, code: String): Boolean
}

interface WebLoginAttemptPort {
    fun isBlocked(normalizedPhone: String, remoteAddress: String): Boolean
    fun recordFailure(normalizedPhone: String, remoteAddress: String)
    fun clear(normalizedPhone: String)
}