package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.ConsentHistoryEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.AccountRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.ConsentHistoryRepository
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AccountJpaEntityService(
    private val accounts: AccountRepository,
    private val consents: ConsentHistoryRepository,
) {
    fun findById(id: Long): AccountEntity? = accounts.findById(id).orElse(null)
    fun findByPublicId(id: UUID): AccountEntity? = accounts.findByPublicId(id)
    @Transactional fun lockByPublicId(id: UUID): AccountEntity? = accounts.findLockedByPublicId(id)
    fun findByPhoneNormalized(phone: String): AccountEntity? = accounts.findByPhoneNormalized(phone)
    fun findAllByStatus(status: String, pageable: Pageable): Page<AccountEntity> = accounts.findAllByStatus(status, pageable)
    fun findAll(pageable: Pageable): Page<AccountEntity> = accounts.findAll(pageable)
    fun consents(accountId: Long): List<ConsentHistoryEntity> = consents.findAllByAccountIdOrderByConsentedAtDesc(accountId)
    fun hasConsent(accountId: Long, consentType: String): Boolean = consents.existsByAccountIdAndConsentType(accountId, consentType)

    @Transactional fun save(account: AccountEntity): AccountEntity = accounts.save(account)

    @Transactional fun saveConsent(consent: ConsentHistoryEntity): ConsentHistoryEntity = consents.save(consent)
}
