package com.buyeong.umji.api.domain.notification.dto

data class NotificationMessageDto(val title: String, val body: String, val data: Map<String, String>)

fun NotificationEventDto.toMessage(): NotificationMessageDto =
    when (type) {
        NotificationEventType.ORDER_CREATED -> NotificationMessageDto("주문 안내", "주문이 접수되었습니다.", safeData())
        NotificationEventType.ORDER_CANCELLED -> NotificationMessageDto("주문 안내", "주문이 취소되었습니다.", safeData())
        NotificationEventType.PAYMENT_STATUS_CHANGED -> NotificationMessageDto("결제 안내", "주문 결제 상태가 변경되었습니다.", safeData())
        NotificationEventType.TAX_INVOICE_ISSUED -> NotificationMessageDto("세금계산서 안내", "요청하신 세금계산서가 발행되었습니다.", safeData())
        NotificationEventType.SHIPMENT_PREPARING -> NotificationMessageDto("배송 안내", "주문 상품의 배송 준비가 시작되었습니다.", safeData())
        NotificationEventType.SHIPMENT_IN_TRANSIT -> NotificationMessageDto("배송 안내", "주문 상품이 발송되었습니다.", safeData())
        NotificationEventType.SHIPMENT_DELIVERED -> NotificationMessageDto("배송 안내", "주문 상품의 배송이 완료되었습니다.", safeData())
    }

private fun NotificationEventDto.safeData(): Map<String, String> = mapOf("notificationType" to type.name, "orderId" to orderId.toString())