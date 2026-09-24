package com.buyeong.umji.api.persistence.jpa.account

import org.springframework.data.jpa.repository.JpaRepository

interface ConsentHistoryRepository : JpaRepository<ConsentHistoryEntity, Long> {
    fun existsByAccountIdAndConsentType(accountId: Long, consentType: String): Boolean
    fun findAllByAccountIdOrderByConsentedAtDesc(accountId: Long): List<ConsentHistoryEntity>
}