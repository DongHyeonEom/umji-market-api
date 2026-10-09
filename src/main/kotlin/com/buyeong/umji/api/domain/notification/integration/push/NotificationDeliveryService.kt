package com.buyeong.umji.api.domain.notification.integration.push

import com.buyeong.umji.api.domain.notification.dto.NotificationDeliveryResultDto
import com.buyeong.umji.api.domain.notification.dto.NotificationEventDto
import com.buyeong.umji.api.domain.notification.dto.NotificationPermanentFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderAcceptedDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderInvalidTokenDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderPermanentFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderRetryableFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationRetryableFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationSentDto
import com.buyeong.umji.api.domain.notification.dto.toMessage
import com.buyeong.umji.api.domain.notification.integration.apns.ApnsHttpPushProvider
import com.buyeong.umji.api.domain.notification.integration.fcm.FirebaseMessagingPushProvider
import com.buyeong.umji.api.domain.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationDeviceTokenJpaEntityService

class NotificationDeliveryService(
    private val deviceTokens: NotificationDeviceTokenJpaEntityService,
    private val fcm: FirebaseMessagingPushProvider?,
    private val apns: ApnsHttpPushProvider?,
) {
    fun deliver(event: NotificationEventDto): NotificationDeliveryResultDto {
        val recipients = deviceTokens.activeRecipientsForOrder(event.orderId)
        if (recipients.isEmpty()) return NotificationSentDto

        var retryableFailure: String? = null
        var permanentFailure: String? = null
        val message = event.toMessage()
        recipients.forEach { recipient ->
            val result = runCatching {
                when (recipient.platform) {
                    NotificationDevicePlatform.ANDROID_FCM ->
                        fcm?.send(recipient, message) ?: NotificationProviderPermanentFailureDto("PROVIDER_NOT_CONFIGURED")
                    NotificationDevicePlatform.IOS_APNS ->
                        apns?.send(recipient, message) ?: NotificationProviderPermanentFailureDto("PROVIDER_NOT_CONFIGURED")
                }
            }.getOrElse { NotificationProviderRetryableFailureDto("PROVIDER_CALL_FAILED") }
            when (result) {
                NotificationProviderAcceptedDto -> Unit
                NotificationProviderInvalidTokenDto -> deviceTokens.deactivate(recipient.id)
                is NotificationProviderRetryableFailureDto -> retryableFailure = result.code
                is NotificationProviderPermanentFailureDto -> permanentFailure = result.code
            }
        }

        return when {
            retryableFailure != null -> NotificationRetryableFailureDto(retryableFailure!!)
            permanentFailure != null -> NotificationPermanentFailureDto(permanentFailure!!)
            else -> NotificationSentDto
        }
    }
}