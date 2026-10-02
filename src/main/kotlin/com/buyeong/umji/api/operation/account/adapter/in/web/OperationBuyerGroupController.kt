package com.buyeong.umji.api.operation.account.adapter.`in`.web

import com.buyeong.umji.api.account.model.SetBuyerGroupRepresentativeRequest
import com.buyeong.umji.api.operation.account.application.port.`in`.OperationAccountUseCase
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/buyer-groups")
class OperationBuyerGroupController(private val accounts: OperationAccountUseCase) {
    @PutMapping("/{groupId}/representative")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun setRepresentative(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: SetBuyerGroupRepresentativeRequest,
    ) = accounts.setBuyerGroupRepresentative(groupId, request.accountId)
}