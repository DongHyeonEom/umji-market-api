package com.buyeong.umji.api.persistence.jpa.account

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AccountJpaEntityService(
    private val accounts: AccountRepository,
    private val profiles: BusinessProfileRepository,
    private val consents: ConsentHistoryRepository,
) {
    fun findById(id: Long): AccountEntity? = accounts.findById(id).orElse(null)
    fun findByPublicId(id: UUID): AccountEntity? = accounts.findByPublicId(id)
    fun findByPhoneNormalized(phone: String): AccountEntity? = accounts.findByPhoneNormalized(phone)
    fun findAllByStatus(status: String, pageable: Pageable): Page<AccountEntity> = accounts.findAllByStatus(status, pageable)
    fun findAll(pageable: Pageable): Page<AccountEntity> = accounts.findAll(pageable)
    fun profile(accountId: Long): BusinessProfileEntity? = profiles.findByAccountId(accountId)
    fun consents(accountId: Long): List<ConsentHistoryEntity> = consents.findAllByAccountIdOrderByConsentedAtDesc(accountId)
    fun hasConsent(accountId: Long, consentType: String): Boolean = consents.existsByAccountIdAndConsentType(accountId, consentType)

    @Transactional fun save(account: AccountEntity): AccountEntity = accounts.save(account)

    @Transactional fun saveProfile(profile: BusinessProfileEntity): BusinessProfileEntity = profiles.save(profile)

    @Transactional fun saveConsent(consent: ConsentHistoryEntity): ConsentHistoryEntity = consents.save(consent)
}