package com.buyeong.umji.api.account.adapter.`in`.web

import com.buyeong.umji.api.account.model.BuyerGroupOnboardingResponse
import com.buyeong.umji.api.account.model.CreateIndividualGroupRequest
import com.buyeong.umji.api.account.model.CustomerProfileResponse
import com.buyeong.umji.api.account.model.DecideBuyerGroupJoinRequest
import com.buyeong.umji.api.account.model.InviteBuyerGroupMemberRequest
import com.buyeong.umji.api.account.model.RequestBuyerGroupJoinRequest
import com.buyeong.umji.api.account.model.SharedAddressRequest
import com.buyeong.umji.api.account.model.SharedAddressResponse
import com.buyeong.umji.api.account.model.toCommand
import com.buyeong.umji.api.account.model.toResponse
import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import com.buyeong.umji.api.account.model.toResponse as membershipResponse

@RestController
@RequestMapping("/api/account")
@Validated
class CustomerAccountController(
    private val currentAccounts: CurrentAccountPort,
    private val account: TransactionalCustomerAccountUseCase,
    private val groups: TransactionalBuyerGroupMembershipUseCase,
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

    @GetMapping("/groups/current")
    fun currentGroup() = groups.current(currentAccounts.activeAccountPublicId())?.toResponse()

    @GetMapping("/groups/onboarding")
    fun groupOnboarding(): BuyerGroupOnboardingResponse {
        val accountId = currentAccounts.activeAccountPublicId()
        return BuyerGroupOnboardingResponse(
            groups.current(accountId)?.toResponse(),
            groups.invitations(accountId).map { it.toResponse() },
        )
    }

    @PostMapping("/groups/individual")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIndividualGroup(@Valid @RequestBody request: CreateIndividualGroupRequest) =
        groups.createIndividualGroup(currentAccounts.activeAccountPublicId(), request.name).toResponse()

    @GetMapping("/groups/search")
    fun searchGroups(@RequestParam @NotBlank phone: String) = groups.search(phone).map { it.membershipResponse() }

    @PostMapping("/groups/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(@Valid @RequestBody request: InviteBuyerGroupMemberRequest) =
        groups.invite(currentAccounts.activeAccountPublicId(), request.phone).toResponse()

    @GetMapping("/groups/invitations")
    fun invitations() = groups.invitations(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @PostMapping("/groups/invitations/{invitationId}/response")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun respondInvitation(
        @PathVariable invitationId: UUID,
        @Valid @RequestBody request: DecideBuyerGroupJoinRequest,
    ) = groups.respondInvitation(currentAccounts.activeAccountPublicId(), invitationId, request.approved)

    @PostMapping("/groups/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestToJoin(@Valid @RequestBody request: RequestBuyerGroupJoinRequest) =
        groups.requestToJoin(currentAccounts.activeAccountPublicId(), request.groupId)

    @GetMapping("/groups/join-requests")
    fun pendingJoinRequests() = groups.pendingJoinRequests(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @PostMapping("/groups/join-requests/{requestId}/response")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun respondJoinRequest(
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: DecideBuyerGroupJoinRequest,
    ) = groups.respondJoinRequest(currentAccounts.activeAccountPublicId(), requestId, request.approved)
}