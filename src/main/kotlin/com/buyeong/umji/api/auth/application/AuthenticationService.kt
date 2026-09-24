package com.buyeong.umji.api.auth.application

import com.buyeong.umji.api.auth.application.model.AccountRecord
import com.buyeong.umji.api.auth.application.model.AuthenticatedAccount
import com.buyeong.umji.api.auth.application.model.AuthenticationStatus
import com.buyeong.umji.api.auth.application.model.IssuedTokens
import com.buyeong.umji.api.auth.application.model.LoginResult
import com.buyeong.umji.api.auth.application.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.application.model.RefreshSessionRecord
import com.buyeong.umji.api.auth.application.model.RefreshTokenCommand
import com.buyeong.umji.api.auth.application.model.RevokeRefreshTokenCommand
import com.buyeong.umji.api.auth.application.port.`in`.AuthenticationUseCase
import com.buyeong.umji.api.auth.application.port.out.AccessTokenIssuerPort
import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.auth.application.port.out.RefreshSessionPort
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.util.PhoneNumberHelper
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

class AuthenticationService(
    private val accounts: AccountAuthenticationPort,
    private val refreshSessions: RefreshSessionPort,
    private val accessTokens: AccessTokenIssuerPort,
) : AuthenticationUseCase {
    override fun login(command: PhoneLoginCommand): LoginResult {
        val phone = PhoneNumberHelper.normalizeMobilePhoneNumber(command.phone)
        val account = accounts.findByNormalizedPhone(phone)
            ?: return LoginResult(AuthenticationStatus.PHONE_VERIFICATION_REQUIRED)
        if (account.status != ACTIVE) return LoginResult(AuthenticationStatus.PHONE_VERIFICATION_REQUIRED, accountResponse(account))
        accounts.recordLogin(account.id, Instant.now())
        return LoginResult(AuthenticationStatus.AUTHENTICATED, accountResponse(account), createTokenPair(account, command.deviceId))
    }

    override fun refresh(command: RefreshTokenCommand): IssuedTokens {
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
        return createTokenPair(session.account, command.deviceId ?: session.deviceId)
    }

    override fun revoke(command: RevokeRefreshTokenCommand) {
        refreshSessions.findLockedByHash(hash(command.refreshToken))?.let { session ->
            if (session.revokedAt == null) refreshSessions.save(session.copy(revokedAt = Instant.now()))
        }
    }

    private fun createTokenPair(account: AccountRecord, deviceId: String?): IssuedTokens {
        val now = Instant.now()
        val accessExpiresAt = now.plus(ACCESS_TOKEN_TTL)
        val accessToken = accessTokens.issue(account, now, accessExpiresAt)
        val refreshToken = newOpaqueToken()
        val refreshExpiresAt = now.plus(REFRESH_TOKEN_TTL)
        refreshSessions.save(
            RefreshSessionRecord(hash(refreshToken), account, deviceId?.trim()?.ifBlank { null }, null, null, refreshExpiresAt),
        )
        return IssuedTokens(accessToken, accessExpiresAt, refreshToken)
    }

    private fun accountResponse(account: AccountRecord) = AuthenticatedAccount(account.id, account.name, account.status)
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