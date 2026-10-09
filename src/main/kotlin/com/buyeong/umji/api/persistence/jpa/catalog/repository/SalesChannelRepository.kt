package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesChannelEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SalesChannelRepository : JpaRepository<SalesChannelEntity, Long> {
    fun findByCode(code: String): SalesChannelEntity?
}