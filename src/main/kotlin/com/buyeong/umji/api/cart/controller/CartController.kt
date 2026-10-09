package com.buyeong.umji.api.cart.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.cart.dto.AddCartItemCommandDto
import com.buyeong.umji.api.cart.dto.CartViewDto
import com.buyeong.umji.api.cart.dto.UpdateCartItemCommandDto
import com.buyeong.umji.api.cart.model.AddCartItemRequest
import com.buyeong.umji.api.cart.model.CartItemResponse
import com.buyeong.umji.api.cart.model.CartResponse
import com.buyeong.umji.api.cart.model.UpdateCartItemRequest
import com.buyeong.umji.api.cart.service.CartService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/cart")
@Tag(name = "장바구니", description = "구매자 그룹 장바구니 조회와 상품 수량 변경 API")
class CartController(
    private val currentAccounts: CurrentAccountService,
    private val carts: CartService,
) {
    @Operation(summary = "장바구니 조회", description = "장바구니 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping
    fun cart(): CartResponse = carts.cart(currentAccounts.activeAccountPublicId()).toResponse()

    @Operation(summary = "장바구니 상품 추가", description = "장바구니 상품 추가 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@Valid @RequestBody request: AddCartItemRequest): CartResponse = carts.add(
        currentAccounts.activeAccountPublicId(),
        AddCartItemCommandDto(request.skuId, request.salesOfferId, request.channelCode, request.quantity),
    ).toResponse()

    @Operation(summary = "장바구니 상품 수량 변경", description = "장바구니 상품 수량 변경 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PatchMapping("/items/{itemId}")
    fun update(@Parameter(description = "대상 장바구니 항목 공개 식별자(UUID)") @PathVariable itemId: UUID, @Valid @RequestBody request: UpdateCartItemRequest): CartResponse =
        carts.update(currentAccounts.activeAccountPublicId(), itemId, UpdateCartItemCommandDto(request.quantity)).toResponse()

    @Operation(summary = "장바구니 상품 삭제", description = "장바구니 상품 삭제 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@Parameter(description = "대상 장바구니 항목 공개 식별자(UUID)") @PathVariable itemId: UUID) {
        carts.remove(currentAccounts.activeAccountPublicId(), itemId)
    }

    private fun CartViewDto.toResponse() = CartResponse(
        items.map {
            CartItemResponse(
                it.id,
                it.skuId,
                it.skuCode,
                it.productName,
                it.skuName,
                it.quantity,
                it.unitPrice,
                it.salesStatus,
                it.salesOfferId,
                it.channelCode,
                it.unitsPerSale,
                it.sellerOrganizationId,
            )
        },
    )
}