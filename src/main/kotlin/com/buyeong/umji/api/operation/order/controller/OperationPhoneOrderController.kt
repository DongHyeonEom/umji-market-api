package com.buyeong.umji.api.operation.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.order.dto.OperationPhoneOrderSummaryDto
import com.buyeong.umji.api.operation.order.model.OperationPhoneOrderBuyerResponse
import com.buyeong.umji.api.operation.order.model.OperationPhoneOrderRequest
import com.buyeong.umji.api.operation.order.model.OperationPhoneOrderResponse
import com.buyeong.umji.api.order.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/orders/phone-orders")
@Validated
@Tag(name = "운영 전화 주문", description = "관리자가 구매자 대신 전화 주문을 기록하는 API")
class OperationPhoneOrderController(
    private val currentAccounts: CurrentAccountService,
    private val orders: OrderService,
) {
    @GetMapping("/buyers")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    @Operation(summary = "전화 주문 구매자 검색", description = "정확히 입력한 휴대폰 번호의 활성 계정과 활성 구매 Organization 조회")
    fun findBuyer(@RequestParam @NotBlank @Size(max = 30) phone: String): OperationPhoneOrderBuyerResponse {
        val buyer = orders.findAdminPhoneOrderBuyer(phone)
            ?: throw ItemNotFoundException("활성 구매 계정과 구매 Organization을 찾을 수 없습니다.")
        return OperationPhoneOrderBuyerResponse(buyer.accountId, buyer.accountName, buyer.phone, buyer.organizationId, buyer.organizationName)
    }

    @PostMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    @Operation(summary = "전화 주문 등록", description = "활성 구매자 계정과 현재 판매 중인 오퍼 가격·재고를 사용해 주문 생성")
    fun create(@Valid @RequestBody request: OperationPhoneOrderRequest): OperationPhoneOrderResponse =
        OperationPhoneOrderResponse(
            orders.createAdminPhoneOrder(
                currentAccounts.activeAccountPublicId(),
                request.buyerAccountId,
                request.shippingAddress(),
                request.orderLines(),
                request.taxInvoiceRequested,
            ).map {
                OperationPhoneOrderSummaryDto(it.id, it.orderNumber, it.status, it.totalAmount, it.orderedAt, it.sellerOrganizationId)
            },
        )
}