package com.buyeong.umji.api.notification.model
data class NotificationMessage(val title: String, val body: String, val data: Map<String, String>)

fun NotificationEvent.toMessage(): NotificationMessage =
    when (type) {
        NotificationEventType.ORDER_CREATED -> NotificationMessage("주문 안내", "주문이 접수되었습니다.", safeData())
        NotificationEventType.ORDER_CANCELLED -> NotificationMessage("주문 안내", "주문이 취소되었습니다.", safeData())
        NotificationEventType.PAYMENT_STATUS_CHANGED -> NotificationMessage("결제 안내", "주문 결제 상태가 변경되었습니다.", safeData())
        NotificationEventType.TAX_INVOICE_ISSUED -> NotificationMessage("세금계산서 안내", "요청하신 세금계산서가 발행되었습니다.", safeData())
        NotificationEventType.SHIPMENT_PREPARING -> NotificationMessage("배송 안내", "주문 상품의 배송 준비가 시작되었습니다.", safeData())
        NotificationEventType.SHIPMENT_IN_TRANSIT -> NotificationMessage("배송 안내", "주문 상품이 발송되었습니다.", safeData())
        NotificationEventType.SHIPMENT_DELIVERED -> NotificationMessage("배송 안내", "주문 상품의 배송이 완료되었습니다.", safeData())
    }

private fun NotificationEvent.safeData(): Map<String, String> = mapOf("notificationType" to type.name, "orderId" to orderId.toString())
