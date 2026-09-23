package com.buyeong.umji.api.auth.application

import com.buyeong.umji.api.auth.application.model.AccountRecord
import com.buyeong.umji.api.auth.application.model.AuthenticationStatus
import com.buyeong.umji.api.auth.application.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.application.port.out.AccessTokenIssuerPort
import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.auth.application.port.out.RefreshSessionPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class AuthenticationServiceTest : DescribeSpec({
    val accounts = mockk<AccountAuthenticationPort>(relaxed = true)
    val sessions = mockk<RefreshSessionPort>(relaxed = true)
    val tokenIssuer = mockk<AccessTokenIssuerPort>()
    val service = AuthenticationService(accounts, sessions, tokenIssuer)
    val accountId = UUID.randomUUID()

    describe("휴대폰 번호 로그인") {
        it("ACTIVE 계정은 본인 인증 없이 토큰을 발급한다") {
            val account = AccountRecord(accountId, "테스트 회원", "ACTIVE", 0)
            every { accounts.findByNormalizedPhone("01012345678") } returns account
            every { tokenIssuer.issue(any(), any(), any()) } returns "access-token"

            val result = service.login(PhoneLoginCommand("010-1234-5678", "device-1"))

            result.status shouldBe AuthenticationStatus.AUTHENTICATED
            result.account?.id shouldBe accountId
            result.tokens?.accessToken shouldBe "access-token"
            result.tokens?.refreshToken?.isNotBlank() shouldBe true
        }

        it("미등록 번호는 본인 인증 필요 상태를 반환한다") {
            every { accounts.findByNormalizedPhone("01012345678") } returns null

            val result = service.login(PhoneLoginCommand("01012345678", null))

            result.status shouldBe AuthenticationStatus.PHONE_VERIFICATION_REQUIRED
            result.account.shouldBeNull()
            result.tokens.shouldBeNull()
        }
    }
})
