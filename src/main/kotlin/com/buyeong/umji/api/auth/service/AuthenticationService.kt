package com.buyeong.umji.api.auth.service

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.account.persistence.AccountRepository
import com.buyeong.umji.api.auth.config.JwtProperties
import com.buyeong.umji.api.auth.model.AuthenticatedAccountResponse
import com.buyeong.umji.api.auth.model.LoginStatus
import com.buyeong.umji.api.auth.model.PhoneLoginRequest
import com.buyeong.umji.api.auth.model.PhoneLoginResponse
import com.buyeong.umji.api.auth.model.RefreshTokenRequest
import com.buyeong.umji.api.auth.model.RevokeRefreshTokenRequest
import com.buyeong.umji.api.auth.model.TokenPairResponse
import com.buyeong.umji.api.auth.persistence.RefreshTokenEntity
import com.buyeong.umji.api.auth.persistence.RefreshTokenRepository
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.util.PhoneNumberHelper
import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
class AuthenticationService(
    private val accounts: AccountRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val jwtEncoderProvider: ObjectProvider<JwtEncoder>,
) {
    @Transactional
    fun login(request: PhoneLoginRequest): PhoneLoginResponse {
        val phone = PhoneNumberHelper.normalizeMobilePhoneNumber(request.phone)
        val account = accounts.findByPhoneNormalized(phone)
            ?: return PhoneLoginResponse(LoginStatus.PHONE_VERIFICATION_REQUIRED)

        if (account.status != ACTIVE) {
            return PhoneLoginResponse(LoginStatus.PHONE_VERIFICATION_REQUIRED, accountResponse(account))
        }

        account.lastLoginAt = Instant.now()
        return PhoneLoginResponse(LoginStatus.AUTHENTICATED, accountResponse(account), createTokenPair(account, request.deviceId))
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): TokenPairResponse {
        val token = refreshTokens.findLockedByTokenHash(hash(request.refreshToken))
            ?: throw ClientBadRequestException("유효하지 않은 Refresh Token입니다.")
        if (token.revokedAt != null || token.account.status != ACTIVE) {
            throw ClientBadRequestException("세션이 만료되었거나 사용할 수 없습니다.")
        }
        if (request.deviceId != null && token.deviceId != null && request.deviceId != token.deviceId) {
            throw ClientBadRequestException("Refresh Token 기기 정보가 일치하지 않습니다.")
        }

        token.revokedAt = Instant.now()
        token.lastUsedAt = token.revokedAt
        return createTokenPair(token.account, request.deviceId ?: token.deviceId)
    }

    @Transactional
    fun revoke(request: RevokeRefreshTokenRequest) {
        refreshTokens.findLockedByTokenHash(hash(request.refreshToken))?.let { token ->
            if (token.revokedAt == null) token.revokedAt = Instant.now()
        }
    }

    private fun createTokenPair(account: AccountEntity, deviceId: String?): TokenPairResponse {
        val now = Instant.now()
        val expiresAt = now.plus(ACCESS_TOKEN_TTL)
        val accessToken = requireNotNull(jwtEncoderProvider.ifAvailable) { "JWT 발급 설정이 필요합니다." }
            .encode(JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer(jwtProperties.issuer)
                    .audience(listOf(jwtProperties.audience))
                    .subject(requireNotNull(account.publicId).toString())
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .claim("tokenVersion", account.tokenVersion)
                    .build(),
            )).tokenValue
        val refreshToken = newOpaqueToken()
        refreshTokens.save(RefreshTokenEntity().apply {
            this.account = account
            tokenHash = hash(refreshToken)
            this.deviceId = deviceId?.trim()?.ifBlank { null }
        })
        return TokenPairResponse(accessToken, expiresAt, refreshToken)
    }

    private fun accountResponse(account: AccountEntity) =
        AuthenticatedAccountResponse(requireNotNull(account.publicId), account.name, account.status)

    private fun newOpaqueToken(): String = ByteArray(REFRESH_TOKEN_BYTES).also(random::nextBytes).let(base64UrlEncoder::encodeToString)

    private fun hash(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val REFRESH_TOKEN_BYTES = 32
        val ACCESS_TOKEN_TTL: Duration = Duration.ofMinutes(15)
        val random = SecureRandom()
        val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    }
}
