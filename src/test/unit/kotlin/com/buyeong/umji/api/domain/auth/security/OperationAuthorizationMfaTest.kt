package com.buyeong.umji.api.domain.auth.security

import com.buyeong.umji.api.domain.auth.config.AuthenticationMode
import com.buyeong.umji.api.domain.auth.config.AuthenticationProperties
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

class OperationAuthorizationMfaTest : DescribeSpec({
    val authorization = OperationAuthorization(AuthenticationProperties(AuthenticationMode.REQUIRED))
    val now = Instant.parse("2026-01-01T00:00:00Z")

    fun authentication(mfaRequired: Boolean, mfaVerified: Boolean): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("00000000-0000-0000-0000-000000000001")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .claim("mfaRequired", mfaRequired)
            .claim("mfaVerified", mfaVerified)
            .build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ORDER_WRITE")))
    }

    describe("운영 API의 관리자 MFA 인가") {
        it("MFA 대상 관리자는 완료 claim이 없으면 거부한다") {
            authorization.hasPermission(authentication(true, false), "ORDER_WRITE") shouldBe false
        }

        it("MFA 대상 관리자는 완료 claim과 permission이 있으면 허용한다") {
            authorization.hasPermission(authentication(true, true), "ORDER_WRITE") shouldBe true
        }

        it("배송·영업 전용 사용자는 별도 MFA 없이 기존 permission 검사를 적용한다") {
            authorization.hasPermission(authentication(false, false), "ORDER_WRITE") shouldBe true
        }
    }
})