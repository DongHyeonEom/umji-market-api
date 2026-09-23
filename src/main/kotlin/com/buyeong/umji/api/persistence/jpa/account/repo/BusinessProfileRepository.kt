package com.buyeong.umji.api.persistence.jpa.account

import org.springframework.data.jpa.repository.JpaRepository

interface BusinessProfileRepository : JpaRepository<BusinessProfileEntity, Long> { fun findByAccountId(accountId: Long): BusinessProfileEntity? }
