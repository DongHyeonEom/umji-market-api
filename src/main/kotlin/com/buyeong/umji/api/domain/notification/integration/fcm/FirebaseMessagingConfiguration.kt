package com.buyeong.umji.api.domain.notification.integration.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "umji.notification.fcm", name = ["enabled"], havingValue = "true")
class FirebaseMessagingConfiguration {
    @Bean
    fun firebaseMessaging(@Value("\${umji.notification.fcm.project-id:}") projectId: String): FirebaseMessaging {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .apply { if (projectId.isNotBlank()) setProjectId(projectId) }
            .build()
        val app = FirebaseApp.getApps().firstOrNull { it.name == FIREBASE_APP_NAME }
            ?: FirebaseApp.initializeApp(options, FIREBASE_APP_NAME)
        return FirebaseMessaging.getInstance(app)
    }

    @Bean
    fun firebaseMessagingPushProvider(messaging: FirebaseMessaging): FirebaseMessagingPushProvider =
        FirebaseMessagingPushProvider(messaging)

    private companion object {
        const val FIREBASE_APP_NAME = "umji-market-notification"
    }
}