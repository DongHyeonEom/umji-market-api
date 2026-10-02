package com.buyeong.umji.api.account.adapter.`in`.web

import com.buyeong.umji.api.account.model.CustomerProfileResponse
import com.buyeong.umji.api.account.model.SharedAddressRequest
import com.buyeong.umji.api.account.model.SharedAddressResponse
import com.buyeong.umji.api.account.model.toCommand
import com.buyeong.umji.api.account.model.toResponse
import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/account")
@Validated
class CustomerAccountController(
    private val currentAccounts: CurrentAccountPort,
    private val account: TransactionalCustomerAccountUseCase,
) {
    @GetMapping("/profile")
    fun profile(): CustomerProfileResponse = account.profile(currentAccounts.activeAccountPublicId()).toResponse()

    @GetMapping("/addresses")
    fun addresses(): List<SharedAddressResponse> = account.addresses(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAddress(@Valid @RequestBody request: SharedAddressRequest): SharedAddressResponse =
        account.createAddress(currentAccounts.activeAccountPublicId(), request.toCommand()).toResponse()

    @PutMapping("/addresses/{addressId}")
    fun updateAddress(
        @PathVariable addressId: UUID,
        @Valid @RequestBody request: SharedAddressRequest,
    ): SharedAddressResponse = account.updateAddress(currentAccounts.activeAccountPublicId(), addressId, request.toCommand()).toResponse()

    @PutMapping("/addresses/{addressId}/default")
    fun setDefaultAddress(@PathVariable addressId: UUID): SharedAddressResponse =
        account.setDefaultAddress(currentAccounts.activeAccountPublicId(), addressId).toResponse()

    @DeleteMapping("/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAddress(@PathVariable addressId: UUID) = account.deleteAddress(currentAccounts.activeAccountPublicId(), addressId)
}