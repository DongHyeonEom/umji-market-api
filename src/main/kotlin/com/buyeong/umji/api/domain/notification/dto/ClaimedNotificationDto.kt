package com.buyeong.umji.api.domain.notification.dto

data class ClaimedNotificationDto(val event: NotificationEventDto, val attemptCount: Int)