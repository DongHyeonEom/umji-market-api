package com.buyeong.umji.api.order.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.order.application.port.`in`.ShippingHolidayUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/operation/shipping-holidays")
class OperationShippingHolidayController(private val currentAccounts: CurrentAccountPort, private val holidays: ShippingHolidayUseCase) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun list() = holidays.list().map { ShippingHolidayResponse(it.date, it.description) }

    @PostMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun register(@Valid @RequestBody request: ShippingHolidayRequest): ShippingHolidayResponse {
        val description = request.description?.trim()?.ifBlank { null }
        holidays.register(request.date, description, currentAccounts.activeAccountPublicId())
        return ShippingHolidayResponse(request.date, description)
    }

    @DeleteMapping("/{date}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ORDER_WRITE')")
    fun remove(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate) = holidays.remove(date)
}

data class ShippingHolidayRequest(val date: LocalDate, @field:Size(max = 200) val description: String? = null)
data class ShippingHolidayResponse(val date: LocalDate, val description: String?)