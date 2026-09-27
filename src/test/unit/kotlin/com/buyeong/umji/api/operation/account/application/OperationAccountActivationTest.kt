package com.buyeong.umji.api.operation.account.application

import com.buyeong.umji.api.operation.account.application.model.AccountData
import com.buyeong.umji.api.operation.account.application.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.application.model.ConsentCommand
import com.buyeong.umji.api.operation.account.application.port.out.OperationAccountPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OperationAccountActivationTest : DescribeSpec({
    val accounts = mockk<OperationAccountPort>(relaxed = true)
    val service = OperationAccountService(accounts)
    val accountId = UUID.randomUUID()

    beforeTest {
        clearMocks(accounts)
    }

    describe("운영자 신규 계정 승인 흐름") {
        it("개인정보 동의가 없는 계정 승인을 거부한다") {
            every { accounts.hasConsent(accountId, "PERSONAL_INFORMATION") } returns false

            shouldThrow<IllegalArgumentException> { service.approve(accountId) }

            verify(exactly = 0) { accounts.approve(any()) }
        }

        it("개인정보 동의 이력이 있는 계정을 활성화한다") {
            val activated = account(status = "ACTIVE", tokenVersion = 2)
            every { accounts.hasConsent(accountId, "PERSONAL_INFORMATION") } returns true
            every { accounts.approve(accountId) } returns activated

            service.approve(accountId) shouldBe activated

            verify(exactly = 1) { accounts.approve(accountId) }
        }

        it("동의가 등록되면 프로필 미등록 계정은 프로필 대기로 이동한다") {
            val current = account(status = "PENDING_CONSENT")
            val next = account(status = "PENDING_PROFILE")
            val consent = ConsentCommand("PERSONAL_INFORMATION", "v1", "ONLINE", null)
            every { accounts.find(accountId) } returns current
            every { accounts.addConsent(accountId, consent, "PENDING_PROFILE") } returns next

            service.consent(accountId, consent) shouldBe next

            verify(exactly = 1) { accounts.addConsent(accountId, consent, "PENDING_PROFILE") }
        }

        it("동의 등록 시 프로필이 있는 계정은 운영 검토 대기로 이동한다") {
            val profile = BusinessProfileData("엄지상회", null, null, null, null, "주소", null)
            val current = account(status = "PENDING_CONSENT", profile = profile)
            val next = account(status = "PENDING_REVIEW", profile = profile)
            val consent = ConsentCommand("PERSONAL_INFORMATION", "v1", "ONLINE", "evidence")
            every { accounts.find(accountId) } returns current
            every { accounts.addConsent(accountId, consent, "PENDING_REVIEW") } returns next

            service.consent(accountId, consent) shouldBe next

            verify(exactly = 1) { accounts.addConsent(accountId, consent, "PENDING_REVIEW") }
        }

        it("프로필이 등록되면 프로필 대기 계정은 운영 검토 대기로 이동한다") {
            val current = account(status = "PENDING_PROFILE")
            val profile = BusinessProfileData("엄지상회", null, null, null, null, "주소", null)
            val next = account(status = "PENDING_REVIEW", profile = profile)
            every { accounts.find(accountId) } returns current
            every { accounts.updateProfile(accountId, profile, "PENDING_REVIEW") } returns next

            service.profile(accountId, profile) shouldBe next

            verify(exactly = 1) { accounts.updateProfile(accountId, profile, "PENDING_REVIEW") }
        }
    }
}) {
    companion object {
        private fun account(
            status: String,
            tokenVersion: Long = 0,
            profile: BusinessProfileData? = null,
        ) = AccountData(UUID.randomUUID(), "테스트 회원", "01012345678", null, status, tokenVersion, profile)
    }
}