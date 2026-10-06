package com.buyeong.umji.api.notification.integration.fcm

import com.buyeong.umji.api.notification.model.NotificationDeviceRecipient
import com.buyeong.umji.api.notification.model.NotificationMessage
import com.buyeong.umji.api.notification.model.NotificationProviderResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

class FirebaseMessagingPushProvider(private val messaging: FirebaseMessaging) {
    fun send(recipient: NotificationDeviceRecipient, message: NotificationMessage): NotificationProviderResult =
        try {
            messaging.send(
                Message.builder()
                    .setToken(recipient.token)
                    .setNotification(Notification.builder().setTitle(message.title).setBody(message.body).build())
                    .putAllData(message.data)
                    .build(),
            )
            NotificationProviderResult.Accepted
        } catch (error: FirebaseMessagingException) {
            classify(error)
        } catch (_: Exception) {
            NotificationProviderResult.RetryableFailure("FCM_TRANSPORT_ERROR")
        }

    private fun classify(error: FirebaseMessagingException): NotificationProviderResult {
        val code = error.messagingErrorCode?.name ?: error.errorCode?.name ?: "UNKNOWN"
        return when (code) {
            "UNREGISTERED" -> NotificationProviderResult.InvalidToken
            "INVALID_ARGUMENT" -> NotificationProviderResult.PermanentFailure("FCM_INVALID_ARGUMENT")
            "QUOTA_EXCEEDED", "UNAVAILABLE", "INTERNAL" -> NotificationProviderResult.RetryableFailure("FCM_$code")
            "UNKNOWN" -> NotificationProviderResult.RetryableFailure("FCM_UNKNOWN_ERROR")
            else -> NotificationProviderResult.PermanentFailure("FCM_$code")
        }
    }
}
