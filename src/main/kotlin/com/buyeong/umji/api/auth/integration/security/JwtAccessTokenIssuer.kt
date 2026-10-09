package com.buyeong.umji.api.auth.integration.security

import com.buyeong.umji.api.auth.config.JwtProperties
import com.buyeong.umji.api.auth.dto.AccountRecordDto
import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class JwtAccessTokenIssuer(
    private val properties: JwtProperties,
    private val encoderProvider: ObjectProvider<JwtEncoder>,
) {
    fun issue(account: AccountRecordDto, now: Instant, expiresAt: Instant): String =
        requireNotNull(encoderProvider.ifAvailable) { "JWT 발급 설정이 필요합니다." }
            .encode(
                JwtEncoderParameters.from(
                    JwtClaimsSet.builder()
                        .issuer(properties.issuer)
                        .audience(listOf(properties.audience))
                        .subject(account.id.toString())
                        .issuedAt(now)
                        .expiresAt(expiresAt)
                        .claim("tokenVersion", account.tokenVersion)
                        .claim("permissions", account.permissions.sorted())
                        .claim("mfaRequired", account.roles.any { it in PRIVILEGED_ROLES })
                        .claim("mfaVerified", account.mfaVerified)
                        .build(),
                ),
            ).tokenValue

    private companion object {
        val PRIVILEGED_ROLES = setOf("ADMIN", "SUPER_ADMIN", "PRODUCT_MANAGER", "ORDER_MANAGER", "INVENTORY_MANAGER")
    }
}