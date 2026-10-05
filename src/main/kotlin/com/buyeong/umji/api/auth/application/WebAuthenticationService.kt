package com.buyeong.umji.api.auth.application

import com.buyeong.umji.api.auth.application.model.AuthenticatedAccount
import com.buyeong.umji.api.auth.application.model.IssuedTokens
import com.buyeong.umji.api.auth.application.model.TotpSetupResult
import com.buyeong.umji.api.auth.application.model.WebLoginCommand
import com.buyeong.umji.api.auth.application.model.WebLoginResult
import com.buyeong.umji.api.auth.application.model.WebPasswordCommand
import com.buyeong.umji.api.auth.application.port.out.AccessTokenIssuerPort
import com.buyeong.umji.api.auth.application.port.out.CredentialEncoderPort
import com.buyeong.umji.api.auth.application.port.out.RefreshSessionPort
import com.buyeong.umji.api.auth.application.port.out.TotpPort
import com.buyeong.umji.api.auth.application.port.out.WebCredentialPort
import com.buyeong.umji.api.auth.application.port.out.WebLoginAttemptPort
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.util.PhoneNumberHelper
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

class WebAuthenticationService(
    private val credentials: WebCredentialPort,
    private val encoder: CredentialEncoderPort,
    private val totp: TotpPort,
    private val refreshSessions: RefreshSessionPort,
    private val accessTokens: AccessTokenIssuerPort,
    private val loginAttempts: WebLoginAttemptPort,
) {
    fun setPassword(command: WebPasswordCommand) {
        validatePassword(command.password)
        val account = credentials.findById(command.accountId) ?: invalid()
        if (account.account.status != ACTIVE) invalid()
        credentials.savePassword(command.accountId, encoder.encode(command.password))
    }

    fun login(command: WebLoginCommand): WebLoginResult {
        val phone = PhoneNumberHelper.normalizeMobilePhoneNumber(command.phone)
        if (loginAttempts.isBlocked(phone, command.remoteAddress)) invalid()
        val found = credentials.findByNormalizedPhone(phone) ?: failed(phone, command.remoteAddress)
        val hash = found.passwordHash ?: failed(phone, command.remoteAddress)
        if (found.account.status != ACTIVE || !encoder.matches(command.password, hash)) failed(phone, command.remoteAddress)
        if (found.roles.any(PRIVILEGED_ROLES::contains)) {
            val secret = found.totpSecret
            if (!found.totpEnabled || secret == null || command.totpCode == null || !totp.verify(secret, command.totpCode)) failed(phone, command.remoteAddress)
        }
        loginAttempts.clear(phone)
        val authenticatedAccount = found.account.copy(mfaVerified = found.roles.any(PRIVILEGED_ROLES::contains))
        val now = Instant.now()
        val pair = IssuedTokens(
            accessTokens.issue(authenticatedAccount, now, now.plus(ACCESS_TOKEN_TTL)),
            now.plus(ACCESS_TOKEN_TTL),
            newRefreshToken(authenticatedAccount, command.deviceId),
        )
        return WebLoginResult(pair, AuthenticatedAccount(found.account.id, found.account.name, found.account.status))
    }

    fun setupTotp(accountId: UUID): TotpSetupResult {
        val account = credentials.findById(accountId) ?: invalid()
        if (account.account.status != ACTIVE || account.roles.none(PRIVILEGED_ROLES::contains)) invalid()
        if (account.totpEnabled) invalid()
        val secret = totp.newSecret()
        credentials.saveTotpSecret(accountId, secret, false)
        return TotpSetupResult(secret, totp.provisioningUri(account.account.name, secret))
    }

    fun confirmTotp(accountId: UUID, code: String) {
        val account = credentials.findById(accountId) ?: invalid()
        if (account.account.status != ACTIVE || account.roles.none(PRIVILEGED_ROLES::contains)) invalid()
        val secret = account.totpSecret ?: invalid()
        if (!totp.verify(secret, code)) invalid()
        credentials.saveTotpSecret(accountId, secret, true)
    }

    fun resetTotp(accountId: UUID) {
        val account = credentials.findById(accountId) ?: invalid()
        if (account.account.status != ACTIVE || account.roles.none(PRIVILEGED_ROLES::contains)) invalid()
        credentials.resetTotp(accountId)
    }

    private fun newRefreshToken(account: com.buyeong.umji.api.auth.application.model.AccountRecord, deviceId: String?): String {
        val value = ByteArray(32).also(random::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)
        refreshSessions.save(
            com.buyeong.umji.api.auth.application.model.RefreshSessionRecord(
                MessageDigest.getInstance("SHA-256").digest(value.toByteArray()),
                account,
                deviceId?.trim()?.ifBlank { null },
                null,
                null,
                Instant.now().plus(REFRESH_TOKEN_TTL),
                account.mfaVerified,
            ),
        )
        return value
    }

    private fun validatePassword(value: String) {
        if (value.length !in 15..128) throw ClientBadRequestException("웹 비밀번호는 15자 이상 128자 이하로 설정해야 합니다.")
    }

    private fun failed(phone: String, remoteAddress: String): Nothing {
        loginAttempts.recordFailure(phone, remoteAddress)
        invalid()
    }

    private fun invalid(): Nothing = throw ClientBadRequestException("로그인 정보 또는 추가 인증 코드가 올바르지 않습니다.")

    private companion object {
        const val ACTIVE = "ACTIVE"
        val PRIVILEGED_ROLES = setOf("ADMIN", "SUPER_ADMIN", "PRODUCT_MANAGER", "ORDER_MANAGER", "INVENTORY_MANAGER")
        val ACCESS_TOKEN_TTL: Duration = Duration.ofHours(1)
        val REFRESH_TOKEN_TTL: Duration = Duration.ofDays(365)
        val random = SecureRandom()
    }
}