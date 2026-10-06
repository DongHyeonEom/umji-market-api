package com.buyeong.umji.api.operation.account.service

import com.buyeong.umji.api.operation.account.model.AccountData
import com.buyeong.umji.api.operation.account.model.OrganizationProfileData
import com.buyeong.umji.api.operation.account.model.ConsentCommand
import com.buyeong.umji.api.operation.account.model.ConsentData
import com.buyeong.umji.api.persistence.jpa.account.service.OperationAccountJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OperationAccountActivationTest : DescribeSpec({
    val accounts = mockk<OperationAccountJpaEntityService>(relaxed = true)
    val service = OperationAccountService(accounts)
    val accountId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()

    fun account(
        status: String,
        tokenVersion: Long = 0,
        profile: OrganizationProfileData? = null,
        consents: List<ConsentData> = emptyList(),
    ) = AccountData(accountId, "테스트 회원", "01012345678", null, status, tokenVersion, profile, consents)

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

        it("개인정보 동의 확인 없이 상태 변경으로 직접 활성화하지 못한다") {
            shouldThrow<IllegalArgumentException> { service.status(accountId, "ACTIVE") }

            verify(exactly = 0) { accounts.updateStatus(any(), any()) }
        }

        it("동의가 등록되면 프로필 미등록 계정은 프로필 대기로 이동한다") {
            val current = account(status = "PENDING_CONSENT")
            val next = account(status = "PENDING_PROFILE")
            val consent = ConsentCommand("PERSONAL_INFORMATION", "v1", "ONLINE", null, operatorId)
            every { accounts.find(accountId) } returns current
            every { accounts.addConsent(accountId, consent, "PENDING_PROFILE") } returns next

            service.consent(accountId, consent) shouldBe next

            verify(exactly = 1) { accounts.addConsent(accountId, consent, "PENDING_PROFILE") }
        }

        it("동의 등록 시 프로필이 있는 계정은 운영 검토 대기로 이동한다") {
            val profile = OrganizationProfileData("엄지상회", null, null, null, null, "주소", null)
            val current = account(status = "PENDING_CONSENT", profile = profile)
            val next = account(status = "PENDING_REVIEW", profile = profile)
            val consent = ConsentCommand("PERSONAL_INFORMATION", "v1", "ONLINE", "evidence", operatorId)
            every { accounts.find(accountId) } returns current
            every { accounts.addConsent(accountId, consent, "PENDING_REVIEW") } returns next

            service.consent(accountId, consent) shouldBe next

            verify(exactly = 1) { accounts.addConsent(accountId, consent, "PENDING_REVIEW") }
        }

        it("프로필이 등록되면 프로필 대기 계정은 운영 검토 대기로 이동한다") {
            val current = account(status = "PENDING_PROFILE")
            val profile = OrganizationProfileData("엄지상회", null, null, null, null, "주소", null)
            val next = account(status = "PENDING_REVIEW", profile = profile)
            every { accounts.find(accountId) } returns current
            every { accounts.updateProfile(accountId, profile, "PENDING_REVIEW") } returns next

            service.profile(accountId, profile) shouldBe next

            verify(exactly = 1) { accounts.updateProfile(accountId, profile, "PENDING_REVIEW") }
        }

        it("운영자는 오프라인 서면 동의를 기록한 뒤 계정을 활성화할 수 있다") {
            val current = account(status = "PENDING_CONSENT")
            val consent = ConsentCommand("PERSONAL_INFORMATION", "privacy-v2", "WRITTEN", "paper-form-2026-001", operatorId)
            val written = ConsentData(
                "PERSONAL_INFORMATION",
                "privacy-v2",
                "WRITTEN",
                "paper-form-2026-001",
                operatorId,
                java.time.Instant.now(),
            )
            val consentRecorded = account(status = "PENDING_PROFILE", consents = listOf(written))
            val activated = account(status = "ACTIVE", tokenVersion = 1, consents = listOf(written))
            every { accounts.find(accountId) } returns current
            every { accounts.addConsent(accountId, consent, "PENDING_PROFILE") } returns consentRecorded
            every { accounts.hasConsent(accountId, "PERSONAL_INFORMATION") } returns true
            every { accounts.approve(accountId) } returns activated

            val result = service.consent(accountId, consent)

            result.consents.single().consentMethod shouldBe "WRITTEN"
            result.consents.single().processedBy shouldBe operatorId
            service.approve(accountId) shouldBe activated

            verify(exactly = 1) { accounts.addConsent(accountId, consent, "PENDING_PROFILE") }
            verify(exactly = 1) { accounts.hasConsent(accountId, "PERSONAL_INFORMATION") }
            verify(exactly = 1) { accounts.approve(accountId) }
        }
    }
})
