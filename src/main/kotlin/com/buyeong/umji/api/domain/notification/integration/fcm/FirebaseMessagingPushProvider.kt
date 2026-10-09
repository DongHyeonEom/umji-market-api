package com.buyeong.umji.api.domain.notification.integration.fcm

import com.buyeong.umji.api.domain.notification.dto.NotificationDeviceRecipientDto
import com.buyeong.umji.api.domain.notification.dto.NotificationMessageDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderAcceptedDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderInvalidTokenDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderPermanentFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderResultDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderRetryableFailureDto
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

class FirebaseMessagingPushProvider(private val messaging: FirebaseMessaging) {
    fun send(recipient: NotificationDeviceRecipientDto, message: NotificationMessageDto): NotificationProviderResultDto =
        try {
            messaging.send(
                Message.builder()
                    .setToken(recipient.token)
                    .setNotification(Notification.builder().setTitle(message.title).setBody(message.body).build())
                    .putAllData(message.data)
                    .build(),
            )
            NotificationProviderAcceptedDto
        } catch (error: FirebaseMessagingException) {
            classify(error)
        } catch (_: Exception) {
            NotificationProviderRetryableFailureDto("FCM_TRANSPORT_ERROR")
        }

    private fun classify(error: FirebaseMessagingException): NotificationProviderResultDto {
        val code = error.messagingErrorCode?.name ?: error.errorCode?.name ?: "UNKNOWN"
        return when (code) {
            "UNREGISTERED" -> NotificationProviderInvalidTokenDto
            "INVALID_ARGUMENT" -> NotificationProviderPermanentFailureDto("FCM_INVALID_ARGUMENT")
            "QUOTA_EXCEEDED", "UNAVAILABLE", "INTERNAL" -> NotificationProviderRetryableFailureDto("FCM_$code")
            "UNKNOWN" -> NotificationProviderRetryableFailureDto("FCM_UNKNOWN_ERROR")
            else -> NotificationProviderPermanentFailureDto("FCM_$code")
        }
    }
}