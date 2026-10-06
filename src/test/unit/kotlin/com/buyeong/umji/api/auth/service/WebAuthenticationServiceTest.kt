package com.buyeong.umji.api.auth.service

import org.springframework.security.crypto.password.PasswordEncoder
import com.buyeong.umji.api.persistence.jpa.auth.WebLoginAttemptJpaEntityService
import com.buyeong.umji.api.auth.integration.security.JwtAccessTokenIssuer
import com.buyeong.umji.api.auth.integration.security.Rfc6238TotpService
import com.buyeong.umji.api.auth.model.AccountRecord
import com.buyeong.umji.api.auth.model.WebAccountCredentials
import com.buyeong.umji.api.auth.model.WebLoginCommand
import com.buyeong.umji.api.auth.model.WebPasswordCommand
import com.buyeong.umji.api.exception.ClientBadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID

class WebAuthenticationServiceTest : DescribeSpec({
    val credentials = mockk<com.buyeong.umji.api.persistence.jpa.auth.WebCredentialJpaEntityService>(relaxed = true)
    val encoder = mockk<PasswordEncoder>()
    val totp = mockk<Rfc6238TotpService>()
    val sessions = mockk<com.buyeong.umji.api.persistence.jpa.auth.AuthenticationJpaEntityService>(relaxed = true)
    val accessTokens = mockk<JwtAccessTokenIssuer>()
    val attempts = mockk<WebLoginAttemptJpaEntityService>(relaxed = true)
    val service = WebAuthenticationService(credentials, encoder, totp, sessions, accessTokens, attempts)
    val accountId = UUID.randomUUID()
    val regularAccount = WebAccountCredentials(AccountRecord(accountId, "회원", "ACTIVE", 1), "encoded", emptySet())

    beforeTest { clearMocks(credentials, encoder, totp, sessions, accessTokens, attempts) }

    describe("웹 비밀번호 설정") {
        it("15자 이상 비밀번호를 해시해 현재 계정에 저장한다") {
            every { credentials.findById(accountId) } returns regularAccount
            every { encoder.encode("a-long-passphrase-for-web") } returns "argon-hash"

            service.setPassword(WebPasswordCommand(accountId, "a-long-passphrase-for-web"))

            verify { credentials.savePassword(accountId, "argon-hash") }
        }

        it("15자 미만 비밀번호는 저장하지 않는다") {
            shouldThrow<ClientBadRequestException> {
                service.setPassword(WebPasswordCommand(accountId, "short"))
            }
            verify(exactly = 0) { credentials.savePassword(any(), any()) }
        }
    }

    describe("웹 로그인") {
        it("일반 사용자는 휴대폰과 웹 비밀번호로 token을 발급한다") {
            every { credentials.findByNormalizedPhone("01012345678") } returns regularAccount
            every { encoder.matches("a-long-passphrase-for-web", "encoded") } returns true
            every { accessTokens.issue(any(), any(), any()) } returns "access"

            val result = service.login(WebLoginCommand("010-1234-5678", "a-long-passphrase-for-web", null, "web", "127.0.0.1"))

            result.tokens.accessToken shouldBe "access"
            val issuedAccount = slot<AccountRecord>()
            verify { accessTokens.issue(capture(issuedAccount), any(), any()) }
            issuedAccount.captured.mfaVerified shouldBe false
        }

        it("상위 관리자는 유효한 TOTP가 없으면 token을 발급하지 않는다") {
            val admin = regularAccount.copy(roles = setOf("SUPER_ADMIN"), totpSecret = "SECRET", totpEnabled = true)
            every { credentials.findByNormalizedPhone("01012345678") } returns admin
            every { encoder.matches(any(), any()) } returns true
            every { totp.verify("SECRET", "123456") } returns false

            shouldThrow<ClientBadRequestException> {
                service.login(WebLoginCommand("01012345678", "a-long-passphrase-for-web", "123456", null, "127.0.0.1"))
            }
            verify(exactly = 0) { accessTokens.issue(any(), any(), any()) }
            verify { attempts.recordFailure("01012345678", "127.0.0.1") }
        }

        it("요청 한도를 넘긴 전화번호와 주소 조합은 계정 조회 전에 거부한다") {
            every { attempts.isBlocked("01012345678", "127.0.0.1") } returns true

            shouldThrow<ClientBadRequestException> {
                service.login(WebLoginCommand("01012345678", "a-long-passphrase-for-web", null, null, "127.0.0.1"))
            }

            verify(exactly = 0) { credentials.findByNormalizedPhone(any()) }
        }

        it("배송관리자만 보유한 계정에는 TOTP를 요구하지 않는다") {
            val shipping = regularAccount.copy(roles = setOf("SHIPPING_MANAGER"))
            every { credentials.findByNormalizedPhone("01012345678") } returns shipping
            every { encoder.matches(any(), any()) } returns true
            every { accessTokens.issue(any(), any(), any()) } returns "access"

            service.login(WebLoginCommand("01012345678", "a-long-passphrase-for-web", null, null, "127.0.0.1")).tokens.accessToken shouldBe "access"
            verify(exactly = 0) { totp.verify(any(), any()) }
        }
    }

    describe("관리자 TOTP 등록") {
        it("첫 등록 secret을 코드 확인 후 활성화한다") {
            val admin = regularAccount.copy(roles = setOf("ADMIN"), totpSecret = "SECRET", totpEnabled = false)
            every { credentials.findById(accountId) } returns admin
            every { totp.verify("SECRET", "654321") } returns true

            service.confirmTotp(accountId, "654321")

            verify { credentials.saveTotpSecret(accountId, "SECRET", true) }
        }

        it("이미 등록된 TOTP는 초기화 절차를 거쳐야 다시 설정할 수 있다") {
            every { credentials.findById(accountId) } returns regularAccount.copy(roles = setOf("SUPER_ADMIN"), totpSecret = "OLD", totpEnabled = true)

            shouldThrow<ClientBadRequestException> { service.setupTotp(accountId) }
            verify(exactly = 0) { credentials.saveTotpSecret(any(), any(), any()) }
        }

        it("TOTP 초기화는 활성 상위 관리자 계정에만 수행한다") {
            every { credentials.findById(accountId) } returns regularAccount.copy(roles = setOf("ADMIN"), totpEnabled = true)

            service.resetTotp(accountId)

            verify { credentials.resetTotp(accountId) }
        }
    }
})
