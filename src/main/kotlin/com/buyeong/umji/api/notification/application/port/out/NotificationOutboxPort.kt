package com.buyeong.umji.api.notification.application.port.out

import com.buyeong.umji.api.notification.application.model.NotificationEvent

interface NotificationOutboxPort {
    fun append(event: NotificationEvent)
}