package com.buyeong.umji.api.persistence.jpa.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingAssignmentDto
import com.buyeong.umji.api.domain.shipment.dto.BrandShippingRateDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.entity.BrandShippingAssignmentEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OperationAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.repository.BrandShippingAssignmentRepository
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandShippingRateEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.repository.BrandShippingRateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BrandShippingJpaEntityService(
    private val catalog: CatalogJpaEntityService,
    private val accounts: AccountJpaEntityService,
    private val assignments: BrandShippingAssignmentRepository,
    private val rates: BrandShippingRateRepository,
    private val operationAccounts: OperationAccountJpaEntityService,
) {
    @Transactional
    fun assignManager(brandId: UUID, accountId: UUID, assignedBy: UUID): BrandShippingAssignmentDto =
        assignStaff(brandId, accountId, assignedBy, MANAGER, SHIPPING_MANAGER)

    @Transactional
    fun assignOperator(brandId: UUID, accountId: UUID, assignedBy: UUID): BrandShippingAssignmentDto =
        assignStaff(brandId, accountId, assignedBy, OPERATOR, SHIPPING_OPERATOR)

    private fun assignStaff(brandId: UUID, accountId: UUID, assignedBy: UUID, type: String, roleCode: String): BrandShippingAssignmentDto {
        val brand = catalog.brand(brandId) ?: throw ItemNotFoundException("브랜드를 찾을 수 없습니다.")
        require(brand.organization != null) { "판매 Organization에 속한 브랜드만 배송 담당자를 지정할 수 있습니다." }
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        require(account.status == ACTIVE) { "활성 계정만 배송 담당자로 지정할 수 있습니다." }
        val assigner = accounts.findByPublicId(assignedBy) ?: throw ItemNotFoundException("처리자 계정을 찾을 수 없습니다.")
        val existing = assignments.findByBrand_PublicIdAndAccount_PublicId(brandId, accountId)
        val assignment = existing
            ?: BrandShippingAssignmentEntity().apply {
                this.brand = brand
                this.account = account
                this.assignedBy = assigner
            }
        val previousType = existing?.assignmentType
        assignment.assignmentType = type
        assignments.saveAndFlush(assignment)
        if (previousType != null && previousType != type) revokeRoleIfUnassigned(accountId, previousType)
        operationAccounts.grantRole(accountId, roleCode, assignedBy)
        return BrandShippingAssignmentDto(brandId, accountId, account.name)
    }

    @Transactional
    fun removeManager(brandId: UUID, accountId: UUID) = removeStaff(brandId, accountId, MANAGER)

    @Transactional
    fun removeOperator(brandId: UUID, accountId: UUID) = removeStaff(brandId, accountId, OPERATOR)

    private fun removeStaff(brandId: UUID, accountId: UUID, type: String) {
        val existing = assignments.findByBrand_PublicIdAndAccount_PublicIdAndAssignmentType(brandId, accountId, type)
        if (existing != null) {
            assignments.delete(existing)
            assignments.flush()
            revokeRoleIfUnassigned(accountId, type)
        }
    }

    private fun revokeRoleIfUnassigned(accountId: UUID, type: String) {
        if (assignments.existsByAccount_PublicIdAndAssignmentType(accountId, type)) return
        operationAccounts.revokeRole(accountId, if (type == MANAGER) SHIPPING_MANAGER else SHIPPING_OPERATOR)
    }

    fun staff(brandId: UUID, type: String): List<BrandShippingAssignmentDto> = assignments.findAllByBrand_PublicIdAndAssignmentType(brandId, type).map {
        BrandShippingAssignmentDto(brandId, requireNotNull(it.account.publicId), it.account.name)
    }

    fun isAssigned(brandId: UUID, accountId: UUID): Boolean =
        assignments.findByBrand_PublicIdAndAccount_PublicId(brandId, accountId) != null

    fun isManagerAssigned(brandId: UUID, accountId: UUID): Boolean =
        assignments.existsByBrand_PublicIdAndAccount_PublicIdAndAssignmentType(brandId, accountId, MANAGER)

    fun assignedBrandIds(accountId: UUID): Set<UUID> = assignments.findAllByAccount_PublicId(accountId)
        .mapNotNull { it.brand.publicId }
        .toSet()

    @Transactional
    fun setRate(brandId: UUID, skuId: UUID, carrierCode: String, amountPerBox: Long, operatorId: UUID): BrandShippingRateDto {
        require(amountPerBox >= 0) { "박스 운송비는 0원 이상이어야 합니다." }
        val brand = catalog.brand(brandId) ?: throw ItemNotFoundException("브랜드를 찾을 수 없습니다.")
        val sku = catalog.sku(skuId) ?: throw ItemNotFoundException("품목을 찾을 수 없습니다.")
        require(sku.product.brand?.id == brand.id) { "품목이 해당 브랜드에 속하지 않습니다." }
        val operator = accounts.findByPublicId(operatorId) ?: throw ItemNotFoundException("처리자 계정을 찾을 수 없습니다.")
        val carrier = carrierCode.trim().uppercase()
        require(carrier.isNotEmpty() && carrier.length <= MAX_CARRIER_LENGTH) { "택배사 코드는 1~80자여야 합니다." }
        val rate = rates.findByBrand_PublicIdAndSku_PublicIdAndCarrierCode(brandId, skuId, carrier)
            ?: BrandShippingRateEntity().apply {
                this.brand = brand
                this.sku = sku
                this.carrierCode = carrier
                createdAt = Instant.now()
            }
        rate.amountPerBox = amountPerBox
        rate.updatedBy = operator
        rate.updatedAt = Instant.now()
        return rates.saveAndFlush(rate).toDto()
    }

    fun rates(brandId: UUID): List<BrandShippingRateDto> = rates.findAllByBrand_PublicId(brandId).map { it.toDto() }

    fun rate(brandId: UUID, skuId: UUID, carrierCode: String): BrandShippingRateDto? =
        rates.findByBrand_PublicIdAndSku_PublicIdAndCarrierCode(brandId, skuId, carrierCode.trim().uppercase())?.toDto()

    private fun BrandShippingRateEntity.toDto() = BrandShippingRateDto(
        brandId = requireNotNull(brand.publicId),
        skuId = requireNotNull(sku.publicId),
        skuName = sku.name,
        carrierCode = carrierCode,
        amountPerBox = amountPerBox,
    )

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val SHIPPING_MANAGER = "SHIPPING_MANAGER"
        const val SHIPPING_OPERATOR = "SHIPPING_OPERATOR"
        const val MANAGER = "MANAGER"
        const val OPERATOR = "OPERATOR"
        const val MAX_CARRIER_LENGTH = 80
    }
}
