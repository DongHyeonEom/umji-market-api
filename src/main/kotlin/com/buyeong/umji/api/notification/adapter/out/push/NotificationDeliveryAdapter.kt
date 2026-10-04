package com.buyeong.umji.api.notification.adapter.out.push

import com.buyeong.umji.api.notification.application.model.NotificationEvent
import com.buyeong.umji.api.notification.application.model.toMessage
import com.buyeong.umji.api.notification.application.port.out.NotificationDeliveryPort
import com.buyeong.umji.api.notification.application.port.out.NotificationDeliveryResult
import com.buyeong.umji.api.notification.application.port.out.NotificationDeviceTokenStorePort
import com.buyeong.umji.api.notification.application.port.out.NotificationProviderResult
import com.buyeong.umji.api.notification.application.port.out.NotificationPushProviderPort

class NotificationDeliveryAdapter(
    private val deviceTokens: NotificationDeviceTokenStorePort,
    providers: List<NotificationPushProviderPort>,
) : NotificationDeliveryPort {
    private val providersByPlatform = providers.associateBy { it.platform }

    override fun deliver(event: NotificationEvent): NotificationDeliveryResult {
        val recipients = deviceTokens.activeRecipientsForOrder(event.orderId)
        if (recipients.isEmpty()) return NotificationDeliveryResult.Sent

        var retryableFailure: String? = null
        var permanentFailure: String? = null
        val message = event.toMessage()
        recipients.forEach { recipient ->
            val provider = providersByPlatform[recipient.platform]
            val result = if (provider == null) {
                NotificationProviderResult.PermanentFailure("PROVIDER_NOT_CONFIGURED")
            } else {
                runCatching { provider.send(recipient, message) }
                    .getOrElse { NotificationProviderResult.RetryableFailure("PROVIDER_CALL_FAILED") }
            }
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