package com.buyeong.umji.api.order.application.port.out

import com.buyeong.umji.api.order.application.model.BankAccountInstructions
import com.buyeong.umji.api.order.application.model.CheckoutLine
import java.time.Instant
import java.util.UUID

interface CheckoutCartPort {
    fun linesForCheckout(accountId: UUID): List<CheckoutLine>
    fun clear(accountId: UUID)
}

interface InventoryReservationPort {
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?)
}

interface BankAccountInstructionsPort {
    fun standard(): BankAccountInstructions
    fun taxInvoice(): BankAccountInstructions
}
