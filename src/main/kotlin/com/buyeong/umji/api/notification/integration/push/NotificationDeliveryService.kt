package com.buyeong.umji.api.notification.integration.push

import com.buyeong.umji.api.notification.model.NotificationEvent
import com.buyeong.umji.api.notification.model.toMessage
import com.buyeong.umji.api.notification.model.NotificationDeliveryResult
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationDeviceTokenJpaEntityService
import com.buyeong.umji.api.notification.model.NotificationProviderResult
import com.buyeong.umji.api.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.integration.apns.ApnsHttpPushProvider
import com.buyeong.umji.api.notification.integration.fcm.FirebaseMessagingPushProvider

class NotificationDeliveryService(
    private val deviceTokens: NotificationDeviceTokenJpaEntityService,
    private val fcm: FirebaseMessagingPushProvider?,
    private val apns: ApnsHttpPushProvider?,
) {
    fun deliver(event: NotificationEvent): NotificationDeliveryResult {
        val recipients = deviceTokens.activeRecipientsForOrder(event.orderId)
        if (recipients.isEmpty()) return NotificationDeliveryResult.Sent

        var retryableFailure: String? = null
        var permanentFailure: String? = null
        val message = event.toMessage()
        recipients.forEach { recipient ->
            val result = runCatching {
                when (recipient.platform) {
                    NotificationDevicePlatform.ANDROID_FCM ->
                        fcm?.send(recipient, message) ?: NotificationProviderResult.PermanentFailure("PROVIDER_NOT_CONFIGURED")
                    NotificationDevicePlatform.IOS_APNS ->
                        apns?.send(recipient, message) ?: NotificationProviderResult.PermanentFailure("PROVIDER_NOT_CONFIGURED")
                }
            }.getOrElse { NotificationProviderResult.RetryableFailure("PROVIDER_CALL_FAILED") }
            when (result) {
                NotificationProviderResult.Accepted -> Unit
                NotificationProviderResult.InvalidToken -> deviceTokens.deactivate(recipient.id)
                is NotificationProviderResult.RetryableFailure -> retryableFailure = result.code
                is NotificationProviderResult.PermanentFailure -> permanentFailure = result.code
            }
        }

        return when {
            retryableFailure != null -> NotificationDeliveryResult.RetryableFailure(retryableFailure!!)
            permanentFailure != null -> NotificationDeliveryResult.PermanentFailure(permanentFailure!!)
            else -> NotificationDeliveryResult.Sent
        }
    }
}
