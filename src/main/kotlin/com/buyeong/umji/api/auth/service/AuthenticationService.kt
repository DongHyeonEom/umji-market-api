package com.buyeong.umji.api.auth.service

import com.buyeong.umji.api.auth.dto.AccountRecordDto
import com.buyeong.umji.api.auth.dto.AuthenticatedAccountDto
import com.buyeong.umji.api.auth.dto.AuthenticationStatus
import com.buyeong.umji.api.auth.dto.IssuedTokensDto
import com.buyeong.umji.api.auth.dto.LoginResultDto
import com.buyeong.umji.api.auth.dto.PhoneLoginCommandDto
import com.buyeong.umji.api.auth.dto.RefreshSessionRecordDto
import com.buyeong.umji.api.auth.dto.RefreshTokenCommandDto
import com.buyeong.umji.api.auth.dto.RevokeRefreshTokenCommandDto
import com.buyeong.umji.api.auth.integration.security.JwtAccessTokenIssuer
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.persistence.jpa.auth.service.AuthenticationJpaEntityService
import com.buyeong.umji.api.util.PhoneNumberHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
@Transactional
class AuthenticationService(
    private val accounts: AuthenticationJpaEntityService,
    private val refreshSessions: AuthenticationJpaEntityService,
    private val accessTokens: JwtAccessTokenIssuer,
) {
    fun login(command: PhoneLoginCommandDto): LoginResultDto {
        val phone = PhoneNumberHelper.normalizeMobilePhoneNumber(command.phone)
        val account = accounts.findByNormalizedPhone(phone)
            ?: return LoginResultDto(AuthenticationStatus.PHONE_VERIFICATION_REQUIRED)
        if (account.status != ACTIVE) return LoginResultDto(AuthenticationStatus.PHONE_VERIFICATION_REQUIRED, accountResponse(account))
        accounts.recordLogin(account.id, Instant.now())
        return LoginResultDto(AuthenticationStatus.AUTHENTICATED, accountResponse(account), createTokenPair(account, command.deviceId))
    }

    fun refresh(command: RefreshTokenCommandDto): IssuedTokensDto {
        val session = refreshSessions.findLockedByHash(hash(command.refreshToken))
            ?: throw ClientBadRequestException("유효하지 않은 Refresh Token입니다.")
        val now = Instant.now()
        if (session.revokedAt != null || session.account.status != ACTIVE || session.expiresAt?.let { !it.isAfter(now) } == true) {
            throw ClientBadRequestException("세션이 만료되었거나 사용할 수 없습니다.")
        }
        if (command.deviceId != null && session.deviceId != null && command.deviceId != session.deviceId) {
            throw ClientBadRequestException("Refresh Token 기기 정보가 일치하지 않습니다.")
        }
        refreshSessions.save(session.copy(revokedAt = now, lastUsedAt = now))
        return createTokenPair(session.account.copy(mfaVerified = session.mfaVerified), command.deviceId ?: session.deviceId)
    }

    fun revoke(command: RevokeRefreshTokenCommandDto) {
        refreshSessions.findLockedByHash(hash(command.refreshToken))?.let { session ->
            if (session.revokedAt == null) refreshSessions.save(session.copy(revokedAt = Instant.now()))
        }
    }

    private fun createTokenPair(account: AccountRecordDto, deviceId: String?): IssuedTokensDto {
        val now = Instant.now()
        val accessExpiresAt = now.plus(ACCESS_TOKEN_TTL)
        val accessToken = accessTokens.issue(account, now, accessExpiresAt)
        val refreshToken = newOpaqueToken()
        val refreshExpiresAt = now.plus(REFRESH_TOKEN_TTL)
        refreshSessions.save(
            RefreshSessionRecordDto(hash(refreshToken), account, deviceId?.trim()?.ifBlank { null }, null, null, refreshExpiresAt, account.mfaVerified),
        )
        return IssuedTokensDto(accessToken, accessExpiresAt, refreshToken)
    }

    private fun accountResponse(account: AccountRecordDto) = AuthenticatedAccountDto(account.id, account.name, account.status)
    private fun newOpaqueToken(): String = ByteArray(REFRESH_TOKEN_BYTES).also(random::nextBytes).let(base64UrlEncoder::encodeToString)
    private fun hash(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val REFRESH_TOKEN_BYTES = 32
        val ACCESS_TOKEN_TTL: Duration = Duration.ofHours(1)
        val REFRESH_TOKEN_TTL: Duration = Duration.ofDays(365)
        val random = SecureRandom()
        val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    }
}