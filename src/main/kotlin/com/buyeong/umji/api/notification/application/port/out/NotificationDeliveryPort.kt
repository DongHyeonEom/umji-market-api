package com.buyeong.umji.api.notification.application.port.out

import com.buyeong.umji.api.notification.application.model.NotificationEvent

interface NotificationDeliveryPort {
    fun deliver(event: NotificationEvent): NotificationDeliveryResult
}

sealed interface NotificationDeliveryResult {
    data object Sent : NotificationDeliveryResult

    data class RetryableFailure(val code: String) : NotificationDeliveryResult

    data class PermanentFailure(val code: String) : NotificationDeliveryResult
}