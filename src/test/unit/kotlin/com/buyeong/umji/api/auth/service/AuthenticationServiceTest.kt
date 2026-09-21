package com.buyeong.umji.api.auth.service

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.account.persistence.AccountRepository
import com.buyeong.umji.api.auth.config.JwtProperties
import com.buyeong.umji.api.auth.model.LoginStatus
import com.buyeong.umji.api.auth.model.PhoneLoginRequest
import com.buyeong.umji.api.auth.persistence.RefreshTokenEntity
import com.buyeong.umji.api.auth.persistence.RefreshTokenRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtEncoder
import java.time.Instant
import java.util.UUID

class AuthenticationServiceTest : DescribeSpec({
    val accounts = mockk<AccountRepository>()
    val refreshTokens = mockk<RefreshTokenRepository>(relaxed = true)
    val jwtEncoder = mockk<JwtEncoder>()
    val jwtEncoderProvider = mockk<ObjectProvider<JwtEncoder>>()
    val properties = JwtProperties("issuer", "audience", "key", "public", "private")
    val service = AuthenticationService(accounts, refreshTokens, properties, jwtEncoderProvider)

    beforeTest {
        every { jwtEncoderProvider.ifAvailable } returns jwtEncoder
        every { jwtEncoder.encode(any()) } returns testJwt()
        every { refreshTokens.save(any<RefreshTokenEntity>()) } answers { firstArg() }
    }

    describe("휴대폰 번호 로그인") {
        it("ACTIVE 계정은 휴대폰 본인 인증 없이 토큰을 발급한다") {
            val account = activeAccount()
            every { accounts.findByPhoneNormalized("01012345678") } returns account

            val response = service.login(PhoneLoginRequest("010-1234-5678", "device-1"))

            response.status shouldBe LoginStatus.AUTHENTICATED
            response.account?.id shouldBe account.publicId
            response.tokens?.accessToken shouldBe "access-token"
            response.tokens?.refreshToken?.isNotBlank() shouldBe true
        }

        it("미등록 번호는 휴대폰 본인 인증 필요 상태를 반환한다") {
            every { accounts.findByPhoneNormalized("01012345678") } returns null

            val response = service.login(PhoneLoginRequest("01012345678"))

            response.status shouldBe LoginStatus.PHONE_VERIFICATION_REQUIRED
            response.account.shouldBeNull()
            response.tokens.shouldBeNull()
        }
    }
}) {
    companion object {
        private fun activeAccount() = AccountEntity().apply {
            publicId = UUID.randomUUID()
            name = "테스트 회원"
            status = "ACTIVE"
            phoneNormalized = "01012345678"
        }

        private fun testJwt(): Jwt =
            Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .subject("subject")
                .build()
    }
}
