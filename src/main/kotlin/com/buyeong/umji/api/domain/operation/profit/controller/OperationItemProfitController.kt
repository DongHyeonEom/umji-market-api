package com.buyeong.umji.api.domain.operation.profit.controller

import com.buyeong.umji.api.domain.operation.profit.model.ItemProfitResponse
import com.buyeong.umji.api.domain.operation.profit.service.OperationItemProfitService
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/operation/profits/items")
@Validated
class OperationItemProfitController(private val profits: OperationItemProfitService) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'INVENTORY_READ')")
    fun query(
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) until: LocalDate,
        @RequestParam(required = false) skuId: UUID?,
        @RequestParam(required = false) brandId: UUID?,
    ): ItemProfitResponse = profits.query(from, until, skuId, brandId)
}
