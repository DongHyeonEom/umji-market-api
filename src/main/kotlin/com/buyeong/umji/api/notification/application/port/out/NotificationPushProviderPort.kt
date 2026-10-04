package com.buyeong.umji.api.notification.application.port.out

import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.model.NotificationDeviceRecipient
import com.buyeong.umji.api.notification.application.model.NotificationMessage

interface NotificationPushProviderPort {
    val platform: NotificationDevicePlatform

    fun send(recipient: NotificationDeviceRecipient, message: NotificationMessage): NotificationProviderResult
}

sealed interface NotificationProviderResult {
    data object Accepted : NotificationProviderResult

    data object InvalidToken : NotificationProviderResult

    data class RetryableFailure(val code: String) : NotificationProviderResult

    data class PermanentFailure(val code: String) : NotificationProviderResult
}