package com.buyeong.umji.api.notification.model

sealed interface NotificationDeliveryResult {
    data object Sent : NotificationDeliveryResult
    data class RetryableFailure(val code: String) : NotificationDeliveryResult
    data class PermanentFailure(val code: String) : NotificationDeliveryResult
}

sealed interface NotificationProviderResult {
    data object Accepted : NotificationProviderResult
    data object InvalidToken : NotificationProviderResult
    data class RetryableFailure(val code: String) : NotificationProviderResult
    data class PermanentFailure(val code: String) : NotificationProviderResult
}

data class ClaimedNotification(val event: NotificationEvent, val attemptCount: Int)
