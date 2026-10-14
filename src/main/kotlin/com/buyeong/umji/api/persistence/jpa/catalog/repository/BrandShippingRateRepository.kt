package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandShippingRateEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BrandShippingRateRepository : JpaRepository<BrandShippingRateEntity, Long> {
    fun findAllByBrand_PublicId(brandId: UUID): List<BrandShippingRateEntity>
    fun findByBrand_PublicIdAndSku_PublicIdAndCarrierCode(brandId: UUID, skuId: UUID, carrierCode: String): BrandShippingRateEntity?
}
