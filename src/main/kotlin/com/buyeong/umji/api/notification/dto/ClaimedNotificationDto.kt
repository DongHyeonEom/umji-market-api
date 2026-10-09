package com.buyeong.umji.api.notification.dto

data class ClaimedNotificationDto(val event: NotificationEventDto, val attemptCount: Int)