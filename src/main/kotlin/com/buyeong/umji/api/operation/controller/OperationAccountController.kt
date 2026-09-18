package com.buyeong.umji.api.operation.controller

import com.buyeong.umji.api.operation.model.*
import com.buyeong.umji.api.operation.service.OperationAccountService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController @RequestMapping("/api/operation/accounts") @Validated
class OperationAccountController(private val service: OperationAccountService) {
    @PostMapping @ResponseStatus(HttpStatus.CREATED) fun create(@Valid @RequestBody r: CreateOperationAccountRequest) = service.create(r)
    @GetMapping fun list(@RequestParam(required = false) status: String?, @RequestParam(defaultValue = "0") @Min(0) page: Int, @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int) = service.list(status, page, size)
    @GetMapping("/{id}") fun detail(@PathVariable id: UUID) = service.detail(id)
    @PatchMapping("/{id}/status") fun status(@PathVariable id: UUID, @Valid @RequestBody r: UpdateAccountStatusRequest) = service.status(id, r)
    @PutMapping("/{id}/business-profile") fun profile(@PathVariable id: UUID, @Valid @RequestBody r: BusinessProfileRequest) = service.profile(id, r)
    @PostMapping("/{id}/consents") fun consent(@PathVariable id: UUID, @Valid @RequestBody r: CreateConsentRequest) = service.consent(id, r)
    @PostMapping("/{id}/approve") fun approve(@PathVariable id: UUID) = service.approve(id)
}
