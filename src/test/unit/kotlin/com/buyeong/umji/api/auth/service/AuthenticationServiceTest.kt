package com.buyeong.umji.api.auth.service

import com.buyeong.umji.api.auth.integration.security.JwtAccessTokenIssuer
import com.buyeong.umji.api.auth.model.AccountRecord
import com.buyeong.umji.api.auth.model.AuthenticationStatus
import com.buyeong.umji.api.auth.model.PhoneLoginCommand
import com.buyeong.umji.api.auth.model.RefreshSessionRecord
import com.buyeong.umji.api.auth.model.RefreshTokenCommand
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.persistence.jpa.auth.service.AuthenticationJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

class AuthenticationServiceTest : DescribeSpec({
    val accounts = mockk<AuthenticationJpaEntityService>(relaxed = true)
    val sessions = accounts
    val tokenIssuer = mockk<JwtAccessTokenIssuer>()
    val service = AuthenticationService(accounts, sessions, tokenIssuer)
    val accountId = UUID.randomUUID()

    beforeTest {
        clearMocks(accounts, sessions, tokenIssuer)
    }

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

        it("Access Token은 1시간, Refresh Token은 기기 정보와 함께 1년 뒤 만료된다") {
            val account = AccountRecord(accountId, "테스트 회원", "ACTIVE", 0)
            every { accounts.findByNormalizedPhone("01012345678") } returns account
            every { tokenIssuer.issue(any(), any(), any()) } returns "access-token"
            val savedSession = io.mockk.slot<RefreshSessionRecord>()
            every { sessions.save(capture(savedSession)) } answers { Unit }
            val before = Instant.now()

            val result = service.login(PhoneLoginCommand("01012345678", " device-1 "))

            val tokens = result.tokens!!
            Duration.between(before.plus(Duration.ofHours(1)), tokens.accessTokenExpiresAt).abs().seconds shouldBe 0L
            Duration.between(before.plus(Duration.ofDays(365)), savedSession.captured.expiresAt!!).abs().seconds shouldBe 0L
            savedSession.captured.deviceId shouldBe "device-1"
            savedSession.captured.revokedAt.shouldBeNull()
            savedSession.captured.lastUsedAt.shouldBeNull()
            savedSession.captured.tokenHash.contentEquals(MessageDigest.getInstance("SHA-256").digest(tokens.refreshToken.toByteArray())) shouldBe true
        }
    }

    describe("Refresh Token 갱신과 만료") {
        it("MFA 검증을 마친 관리자 세션은 refresh 이후에도 MFA 완료 상태를 유지한다") {
            val account = AccountRecord(accountId, "관리자", "ACTIVE", 2, roles = setOf("SUPER_ADMIN"), mfaVerified = true)
            val session = RefreshSessionRecord(
                MessageDigest.getInstance("SHA-256").digest("mfa-refresh".toByteArray()),
                account,
                "admin-browser",
                null,
                null,
                Instant.now().plusSeconds(60),
                mfaVerified = true,
            )
            every { sessions.findLockedByHash(any()) } returns session
            every { tokenIssuer.issue(any(), any(), any()) } returns "new-access"
            val saved = slot<RefreshSessionRecord>()
            every { sessions.save(capture(saved)) } answers { Unit }

            service.refresh(RefreshTokenCommand("mfa-refresh", "admin-browser"))

            saved.captured.mfaVerified shouldBe true
            val issuedAccount = slot<AccountRecord>()
            verify { tokenIssuer.issue(capture(issuedAccount), any(), any()) }
            issuedAccount.captured.mfaVerified shouldBe true
        }

        it("사용한 세션을 폐기하고 새 Refresh Token과 rolling 만료 시각을 저장한다") {
            val account = AccountRecord(accountId, "테스트 회원", "ACTIVE", 0)
            val previous = RefreshSessionRecord(
                MessageDigest.getInstance("SHA-256").digest("old-refresh-token".toByteArray()),
                account,
                "device-1",
                null,
                null,
                Instant.now().plusSeconds(60),
            )
            every { tokenIssuer.issue(any(), any(), any()) } returns "new-access-token"
            val savedSessions = mutableListOf<RefreshSessionRecord>()
            every { sessions.findLockedByHash(any()) } answers {
                savedSessions.firstOrNull()?.takeIf { savedSessions.size == 2 } ?: previous
            }
            every { sessions.save(any()) } answers {
                savedSessions.add(firstArg())
                Unit
            }
            val before = Instant.now()

            val result = service.refresh(RefreshTokenCommand("old-refresh-token", "device-1"))

            savedSessions.size shouldBe 2
            savedSessions[0].revokedAt shouldBe savedSessions[0].lastUsedAt
            Duration.between(savedSessions[0].revokedAt, savedSessions[1].expiresAt?.minus(Duration.ofDays(365))).abs().seconds shouldBe 0L
            savedSessions[1].revokedAt.shouldBeNull()
            savedSessions[1].deviceId shouldBe "device-1"
            savedSessions[1].tokenHash.contentEquals(MessageDigest.getInstance("SHA-256").digest(result.refreshToken.toByteArray())) shouldBe true
            savedSessions[1].tokenHash.contentEquals(previous.tokenHash) shouldBe false
            Duration.between(before.plus(Duration.ofDays(365)), savedSessions[1].expiresAt!!).abs().seconds shouldBe 0L
            verify { sessions.findLockedByHash(match { it.contentEquals(previous.tokenHash) }) }

            shouldThrow<ClientBadRequestException> {
                service.refresh(RefreshTokenCommand("old-refresh-token", "device-1"))
            }
            savedSessions.size shouldBe 2
        }

        it("만료된 Refresh Token은 갱신하지 않는다") {
            val expired = RefreshSessionRecord(
                ByteArray(32),
                AccountRecord(accountId, "테스트 회원", "ACTIVE", 0),
                "device-1",
                null,
                null,
                Instant.now().minusSeconds(1),
            )
            every { sessions.findLockedByHash(any()) } returns expired

            shouldThrow<ClientBadRequestException> {
                service.refresh(RefreshTokenCommand("expired-refresh-token", "device-1"))
            }

            verify(exactly = 0) { sessions.save(any()) }
        }

        it("정지된 계정의 Refresh Token은 갱신하지 않는다") {
            val suspended = RefreshSessionRecord(
                ByteArray(32),
                AccountRecord(accountId, "테스트 회원", "SUSPENDED", 0),
                "device-1",
                null,
                null,
                Instant.now().plus(Duration.ofDays(30)),
            )
            every { sessions.findLockedByHash(any()) } returns suspended

            shouldThrow<ClientBadRequestException> {
                service.refresh(RefreshTokenCommand("suspended-refresh-token", "device-1"))
            }

            verify(exactly = 0) { sessions.save(any()) }
        }

        it("다른 기기의 Refresh Token 요청은 거부한다") {
            val session = RefreshSessionRecord(
                ByteArray(32),
                AccountRecord(accountId, "테스트 회원", "ACTIVE", 0),
                "registered-device",
                null,
                null,
                Instant.now().plus(Duration.ofDays(30)),
            )
            every { sessions.findLockedByHash(any()) } returns session

            shouldThrow<ClientBadRequestException> {
                service.refresh(RefreshTokenCommand("valid-refresh-token", "different-device"))
            }

            verify(exactly = 0) { sessions.save(any()) }
        }
    }
})
