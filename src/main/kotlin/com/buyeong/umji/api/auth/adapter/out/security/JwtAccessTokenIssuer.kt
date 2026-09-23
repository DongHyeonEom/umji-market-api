package com.buyeong.umji.api.auth.adapter.out.security

import com.buyeong.umji.api.auth.application.model.AccountRecord
import com.buyeong.umji.api.auth.application.port.out.AccessTokenIssuerPort
import com.buyeong.umji.api.auth.config.JwtProperties
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
) : AccessTokenIssuerPort {
    override fun issue(account: AccountRecord, now: Instant, expiresAt: Instant): String =
        requireNotNull(encoderProvider.ifAvailable) { "JWT 발급 설정이 필요합니다." }
            .encode(JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer(properties.issuer)
                    .audience(listOf(properties.audience))
                    .subject(account.id.toString())
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .claim("tokenVersion", account.tokenVersion)
                    .claim("permissions", account.permissions.sorted())
                    .build(),
            )).tokenValue
}
