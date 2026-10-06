package com.buyeong.umji.api.persistence.jpa.account.repository

import com.buyeong.umji.api.persistence.jpa.account.entity.BusinessProfileEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BusinessProfileRepository : JpaRepository<BusinessProfileEntity, Long> {
    fun findByAccountId(accountId: Long): BusinessProfileEntity?
}
