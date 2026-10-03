package com.buyeong.umji.api.persistence.jpa.notification

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationOutboxRepository : JpaRepository<NotificationOutboxEntity, Long>