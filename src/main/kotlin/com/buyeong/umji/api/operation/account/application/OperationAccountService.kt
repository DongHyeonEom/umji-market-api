package com.buyeong.umji.api.operation.account.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.account.application.model.*
import com.buyeong.umji.api.operation.account.application.port.`in`.OperationAccountUseCase
import com.buyeong.umji.api.operation.account.application.port.out.OperationAccountPort
import java.util.UUID

class OperationAccountService(private val accounts: OperationAccountPort) : OperationAccountUseCase {
    override fun create(command: NewAccount) = accounts.create(command)
    override fun list(status: String?, page: Int, size: Int) = accounts.list(status, page, size)
    override fun detail(id: UUID) = accounts.find(id) ?: missing()
    override fun status(id: UUID, status: String): AccountData { require(status in STATUSES) { "유효하지 않은 계정 상태입니다." }; return accounts.updateStatus(id, status) ?: missing() }
    override fun profile(id: UUID, profile: BusinessProfileData): AccountData {
        val account = accounts.find(id) ?: missing()
        val nextStatus = if (account.status == PENDING_PROFILE) PENDING_REVIEW else account.status
        return accounts.updateProfile(id, profile, nextStatus) ?: missing()
    }
    override fun consent(id: UUID, consent: ConsentCommand): AccountData {
        require(consent.consentMethod in METHODS) { "유효하지 않은 동의 방식입니다." }
        val account = accounts.find(id) ?: missing()
        val nextStatus = if (account.status == PENDING_CONSENT) {
            if (account.profile == null) PENDING_PROFILE else PENDING_REVIEW
        } else account.status
        return accounts.addConsent(id, consent, nextStatus) ?: missing()
    }
    override fun approve(id: UUID): AccountData {
        require(accounts.hasConsent(id, PERSONAL_INFORMATION)) { "개인정보 동의 이력이 필요합니다." }
        return accounts.approve(id) ?: missing()
    }
    private fun missing(): Nothing = throw ItemNotFoundException("계정을 찾을 수 없습니다.")
    private companion object { const val PENDING_CONSENT = "PENDING_CONSENT"; const val PENDING_PROFILE = "PENDING_PROFILE"; const val PENDING_REVIEW = "PENDING_REVIEW"; const val PERSONAL_INFORMATION = "PERSONAL_INFORMATION"; val STATUSES = setOf(PENDING_CONSENT, PENDING_PROFILE, PENDING_REVIEW, "ACTIVE", "SUSPENDED", "WITHDRAWN"); val METHODS = setOf("ONLINE", "WRITTEN") }
}
