package com.buyeong.umji.api.domain.auth.service

import com.buyeong.umji.api.domain.auth.dto.AccountRecordDto
import com.buyeong.umji.api.domain.auth.dto.AuthenticatedAccountDto
import com.buyeong.umji.api.domain.auth.dto.IssuedTokensDto
import com.buyeong.umji.api.domain.auth.dto.RefreshSessionRecordDto
import com.buyeong.umji.api.domain.auth.dto.TotpSetupResultDto
import com.buyeong.umji.api.domain.auth.dto.WebLoginCommandDto
import com.buyeong.umji.api.domain.auth.dto.WebLoginResultDto
import com.buyeong.umji.api.domain.auth.dto.WebPasswordCommandDto
import com.buyeong.umji.api.domain.auth.integration.security.JwtAccessTokenIssuer
import com.buyeong.umji.api.domain.auth.integration.security.Rfc6238TotpService
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.persistence.jpa.auth.service.AuthenticationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.auth.service.WebCredentialJpaEntityService
import com.buyeong.umji.api.persistence.jpa.auth.service.WebLoginAttemptJpaEntityService
import com.buyeong.umji.api.util.PhoneNumberHelper
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

@org.springframework.stereotype.Service
@Transactional
class WebAuthenticationService(
    private val credentials: WebCredentialJpaEntityService,
    private val encoder: PasswordEncoder,
    private val totp: Rfc6238TotpService,
    private val refreshSessions: AuthenticationJpaEntityService,
    private val accessTokens: JwtAccessTokenIssuer,
    private val loginAttempts: WebLoginAttemptJpaEntityService,
) {
    fun setPassword(command: WebPasswordCommandDto) {
        validatePassword(command.password)
        val account = credentials.findById(command.accountId) ?: invalid()
        if (account.account.status != ACTIVE) invalid()
        credentials.savePassword(command.accountId, encoder.encode(command.password))
    }

    fun login(command: WebLoginCommandDto): WebLoginResultDto {
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
        val pair = IssuedTokensDto(
            accessTokens.issue(authenticatedAccount, now, now.plus(ACCESS_TOKEN_TTL)),
            now.plus(ACCESS_TOKEN_TTL),
            newRefreshToken(authenticatedAccount, command.deviceId),
        )
        return WebLoginResultDto(pair, AuthenticatedAccountDto(found.account.id, found.account.name, found.account.status))
    }

    fun setupTotp(accountId: UUID): TotpSetupResultDto {
        val account = credentials.findById(accountId) ?: invalid()
        if (account.account.status != ACTIVE || account.roles.none(PRIVILEGED_ROLES::contains)) invalid()
        if (account.totpEnabled) invalid()
        val secret = totp.newSecret()
        credentials.saveTotpSecret(accountId, secret, false)
        return TotpSetupResultDto(secret, totp.provisioningUri(account.account.name, secret))
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

    private fun newRefreshToken(account: com.buyeong.umji.api.domain.auth.dto.AccountRecordDto, deviceId: String?): String {
        val value = ByteArray(32).also(random::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)
        refreshSessions.save(
            com.buyeong.umji.api.domain.auth.dto.RefreshSessionRecordDto(
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