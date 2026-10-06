package com.buyeong.umji.api.persistence.jpa.notification.repository

import com.buyeong.umji.api.persistence.jpa.notification.entity.NotificationOutboxEntity
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationOutboxRepository : JpaRepository<NotificationOutboxEntity, Long>
