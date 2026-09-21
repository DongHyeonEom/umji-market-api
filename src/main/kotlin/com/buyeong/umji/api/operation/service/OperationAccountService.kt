package com.buyeong.umji.api.operation.service

import com.buyeong.umji.api.account.persistence.*
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.model.*
import com.buyeong.umji.api.util.PhoneNumberHelper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OperationAccountService(private val accounts: AccountRepository, private val profiles: BusinessProfileRepository, private val consents: ConsentHistoryRepository) {
    fun create(r: CreateOperationAccountRequest): OperationAccountResponse {
        val normalizedPhone = PhoneNumberHelper.normalizeMobilePhoneNumber(r.phone)
        val a = accounts.save(AccountEntity().apply { name = r.name.trim(); phone = r.phone.trim(); phoneNormalized = normalizedPhone; email = r.email?.trim()?.ifBlank { null }; status = PENDING_CONSENT })
        r.businessProfile?.let { profile(a, it) }
        return response(a)
    }
    @Transactional(readOnly = true) fun list(status: String?, page: Int, size: Int): List<OperationAccountResponse> {
        val p = PageRequest.of(page, size, Sort.by("id").descending()); return (if (status.isNullOrBlank()) accounts.findAll(p) else accounts.findAllByStatus(status, p)).content.map(::response)
    }
    @Transactional(readOnly = true) fun detail(id: UUID): OperationAccountResponse = response(account(id), true)
    fun status(id: UUID, r: UpdateAccountStatusRequest): OperationAccountResponse { require(r.status in STATUSES) { "유효하지 않은 계정 상태입니다." }; val a = account(id); if (a.status != r.status) { a.status = r.status; a.tokenVersion += 1 }; return response(a) }
    fun profile(id: UUID, r: BusinessProfileRequest): OperationAccountResponse { val a = account(id); profile(a, r); if (a.status == PENDING_PROFILE) a.status = PENDING_REVIEW; return response(a) }
    fun consent(id: UUID, r: CreateConsentRequest): OperationAccountResponse {
        require(r.consentMethod in METHODS) { "유효하지 않은 동의 방식입니다." }; val a = account(id)
        consents.save(ConsentHistoryEntity().apply { account = a; consentType = r.consentType; documentVersion = r.documentVersion; consentMethod = r.consentMethod; evidenceReference = r.evidenceReference?.trim()?.ifBlank { null }; consentedAt = Instant.now() })
        if (a.status == PENDING_CONSENT) a.status = if (profiles.findByAccountId(requireNotNull(a.id)) == null) PENDING_PROFILE else PENDING_REVIEW
        return response(a, true)
    }
    fun approve(id: UUID): OperationAccountResponse { val a = account(id); require(consents.existsByAccountIdAndConsentType(requireNotNull(a.id), PERSONAL)) { "개인정보 동의 이력이 필요합니다." }; a.status = ACTIVE; a.tokenVersion += 1; return response(a, true) }
    private fun profile(a: AccountEntity, r: BusinessProfileRequest) { val p = profiles.findByAccountId(requireNotNull(a.id)) ?: BusinessProfileEntity().apply { account = a }; p.businessName = r.businessName.trim(); p.businessRegistrationNumber = r.businessRegistrationNumber?.trim()?.ifBlank { null }; p.representativeName = r.representativeName?.trim()?.ifBlank { null }; p.businessPhone = r.businessPhone?.trim()?.ifBlank { null }; p.postalCode = r.postalCode?.trim()?.ifBlank { null }; p.address1 = r.address1?.trim()?.ifBlank { null }; p.address2 = r.address2?.trim()?.ifBlank { null }; p.status = "COMPLETED"; profiles.save(p) }
    private fun response(a: AccountEntity, detail: Boolean = false) = OperationAccountResponse(requireNotNull(a.publicId), a.name, a.phone, a.email, a.status, a.tokenVersion, profiles.findByAccountId(requireNotNull(a.id))?.let { OperationBusinessProfileResponse(it.businessName, it.businessRegistrationNumber, it.representativeName, it.businessPhone, it.postalCode, it.address1, it.address2, it.status) }, if (detail) consents.findAllByAccountIdOrderByConsentedAtDesc(requireNotNull(a.id)).map { OperationConsentResponse(it.consentType, it.documentVersion, it.consentMethod, it.evidenceReference, it.consentedAt) } else emptyList())
    private fun account(id: UUID) = accounts.findByPublicId(id) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
    private companion object { const val PENDING_CONSENT = "PENDING_CONSENT"; const val PENDING_PROFILE = "PENDING_PROFILE"; const val PENDING_REVIEW = "PENDING_REVIEW"; const val ACTIVE = "ACTIVE"; const val PERSONAL = "PERSONAL_INFORMATION"; val STATUSES = setOf(PENDING_CONSENT, PENDING_PROFILE, PENDING_REVIEW, ACTIVE, "SUSPENDED", "WITHDRAWN"); val METHODS = setOf("ONLINE", "WRITTEN") }
}
