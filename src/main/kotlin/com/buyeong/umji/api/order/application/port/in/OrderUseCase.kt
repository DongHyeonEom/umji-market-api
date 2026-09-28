package com.buyeong.umji.api.order.application.port.`in`

import com.buyeong.umji.api.order.application.model.OrderCheckoutOptions
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import java.util.UUID

interface OrderUseCase {
    fun checkoutOptions(accountPublicId: UUID): OrderCheckoutOptions
    fun create(accountPublicId: UUID, taxInvoiceRequested: Boolean?, updateDefaultTaxInvoicePreference: Boolean): OrderView
    fun create(accountPublicId: UUID): OrderView = create(accountPublicId, null, false)
    fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage
    fun detail(accountPublicId: UUID, orderId: UUID): OrderView
}
