package com.buyeong.umji.api.domain.operation.shipment.controller

import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.domain.shipment.model.AssignBrandShippingManagerRequest
import com.buyeong.umji.api.domain.shipment.model.BrandShippingManagerResponse
import com.buyeong.umji.api.domain.shipment.model.BrandShippingRateResponse
import com.buyeong.umji.api.domain.shipment.model.SetBrandShippingRateRequest
import com.buyeong.umji.api.domain.shipment.model.toResponse
import com.buyeong.umji.api.domain.shipment.service.BrandShippingService
import org.springframework.security.access.prepost.PreAuthorize
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/shipping/brands/{brandId}")
class OperationBrandShippingController(
    private val currentAccounts: CurrentAccountService,
    private val shipping: BrandShippingService,
) {
    @GetMapping("/managers")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_ASSIGN')")
    fun managers(@PathVariable brandId: UUID): List<BrandShippingManagerResponse> =
        shipping.managers(brandId).map { it.toResponse() }

    @PutMapping("/managers")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_ASSIGN')")
    fun assignManager(
        @PathVariable brandId: UUID,
        @Valid @RequestBody request: AssignBrandShippingManagerRequest,
    ): BrandShippingManagerResponse = shipping.assignManager(brandId, request.accountId, currentAccounts.activeAccountPublicId()).toResponse()

    @DeleteMapping("/managers/{accountId}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_ASSIGN')")
    fun removeManager(@PathVariable brandId: UUID, @PathVariable accountId: UUID) =
        shipping.removeManager(brandId, accountId)

    @GetMapping("/rates")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_READ')")
    fun rates(@PathVariable brandId: UUID): List<BrandShippingRateResponse> =
        shipping.rates(brandId, currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @PutMapping("/rates")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SHIPMENT_ASSIGN')")
    fun setRate(
        @PathVariable brandId: UUID,
        @Valid @RequestBody request: SetBrandShippingRateRequest,
    ): BrandShippingRateResponse = shipping.setRate(
        brandId, request.skuId, request.carrierCode, request.amountPerBox,
        currentAccounts.activeAccountPublicId(),
    ).toResponse()
}
