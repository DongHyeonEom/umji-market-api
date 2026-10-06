package com.buyeong.umji.api.notification.integration.apns

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "umji.notification.apns", name = ["enabled"], havingValue = "true")
class ApnsConfiguration {
    @Bean
    fun apnsPushProvider(
        @Value("\${umji.notification.apns.team-id}") teamId: String,
        @Value("\${umji.notification.apns.key-id}") keyId: String,
        @Value("\${umji.notification.apns.private-key-path}") privateKeyPath: String,
        @Value("\${umji.notification.apns.topic}") topic: String,
        @Value("\${umji.notification.apns.environment:production}") environment: String,
        objectMapper: ObjectMapper,
    ): ApnsHttpPushProvider = ApnsHttpPushProvider(teamId, keyId, privateKeyPath, topic, environment, objectMapper)
}
