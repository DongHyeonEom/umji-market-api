package com.buyeong.umji.api.operation.account.integration.persistence

import com.buyeong.umji.api.operation.account.model.ConsentCommand
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.ConsentHistoryEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OperationAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class JpaOperationAccountConsentTest {
    private val accounts = mockk<AccountJpaEntityService>(relaxed = true)
    private val adapter = OperationAccountJpaEntityService(accounts, mockk<JdbcTemplate>(relaxed = true), mockk<OrganizationJpaEntityService>(relaxed = true))

    @Test
    fun `written consent stores the processing administrator and returns it in account data`() {
        val accountId = UUID.randomUUID()
        val operatorId = UUID.randomUUID()
        val account = account(id = 1, publicId = accountId)
        val operator = account(id = 2, publicId = operatorId)
        val savedConsent = ConsentHistoryEntity().apply {
            this.account = account
            processedBy = operator
            consentType = "PERSONAL_INFORMATION"
            documentVersion = "privacy-v2"
            consentMethod = "WRITTEN"
            evidenceReference = "paper-form-2026-001"
            consentedAt = Instant.parse("2026-09-28T00:00:00Z")
        }
        val persistedConsent = slot<ConsentHistoryEntity>()
        every { accounts.findByPublicId(accountId) } returns account
        every { accounts.findByPublicId(operatorId) } returns operator
        every { accounts.saveConsent(capture(persistedConsent)) } answers { firstArg() }
        every { accounts.consents(1) } returns listOf(savedConsent)

        val result = adapter.addConsent(
            accountId,
            ConsentCommand("PERSONAL_INFORMATION", "privacy-v2", "WRITTEN", "paper-form-2026-001", operatorId),
            "PENDING_PROFILE",
        )

        assertThat(persistedConsent.captured.processedBy).isSameAs(operator)
        assertThat(result?.consents?.single()?.processedBy).isEqualTo(operatorId)
        verify(exactly = 1) { accounts.saveConsent(any()) }
    }

    private fun account(id: Long, publicId: UUID) =
        AccountEntity().apply {
            this.id = id
            this.publicId = publicId
            name = "테스트 회원"
            status = "PENDING_CONSENT"
        }
}