package com.buyeong.umji.api.account.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface BusinessProfileRepository : JpaRepository<BusinessProfileEntity, Long> { fun findByAccountId(accountId: Long): BusinessProfileEntity? }
