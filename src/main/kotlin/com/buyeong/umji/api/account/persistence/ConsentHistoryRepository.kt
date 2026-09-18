package com.buyeong.umji.api.account.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ConsentHistoryRepository : JpaRepository<ConsentHistoryEntity, Long> {
    fun existsByAccountIdAndConsentType(accountId: Long, consentType: String): Boolean
    fun findAllByAccountIdOrderByConsentedAtDesc(accountId: Long): List<ConsentHistoryEntity>
}
