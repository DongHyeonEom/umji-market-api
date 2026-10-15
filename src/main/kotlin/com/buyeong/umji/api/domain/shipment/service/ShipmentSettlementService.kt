package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentSettlementQueryDto
import com.buyeong.umji.api.persistence.jpa.shipment.service.ShipmentSettlementJpaEntityService
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ShipmentSettlementService(
    private val settlements: ShipmentSettlementJpaEntityService,
    private val brandShipping: BrandShippingService,
) {
    fun monthly(brandId: UUID, month: String, operatorId: UUID, page: Int, size: Int): ShipmentSettlementDto {
        val yearMonth = runCatching { YearMonth.parse(month) }.getOrElse {
            throw IllegalArgumentException("조회 월은 YYYY-MM 형식이어야 합니다.")
        }
        if (!brandShipping.canManageBrandOrAdmin(brandId, operatorId)) {
            throw AccessDeniedException("담당 브랜드의 운송비만 조회할 수 있습니다.")
        }
        val from = yearMonth.atDay(1).atStartOfDay(KST).toInstant()
        val until = runCatching { yearMonth.plusMonths(1) }
            .getOrElse { throw IllegalArgumentException("조회 월이 지원 범위를 벗어났습니다.") }
            .atDay(1)
            .atStartOfDay(KST)
            .toInstant()
        return settlements.monthly(ShipmentSettlementQueryDto(brandId, yearMonth, from, until, page, size))
    }

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
