package com.buyeong.umji.api.notification.controller

import com.buyeong.umji.api.notification.model.NotificationDeviceTokenRegistration
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterNotificationDeviceTokenRequest(
    @field:NotBlank
    @field:Schema(description = "푸시 token 제공 platform", example = "ANDROID_FCM", type = "string", required = true)
    val platform: String,
    @field:NotBlank
    @field:Size(max = 4096)
    @field:Schema(description = "FCM token은 1~4096자, APNs device token은 64자리 16진수", example = "fcm-device-token-example", type = "string", required = true)
    val token: String,
)

data class NotificationDeviceTokenResponse(
    @field:Schema(description = "인증 계정에 연결된 기기 token 공개 식별자(UUID)", example = "550e8400-e29b-41d4-a716-446655440000", type = "string", required = true)
    val id: UUID,
    @field:Schema(description = "푸시 token 제공 platform", example = "ANDROID_FCM", type = "string", required = true)
    val platform: String,
    @field:Schema(description = "마지막 token 등록·갱신 시각(UTC)", example = "2026-10-05T10:00:00Z", type = "string", required = true)
    val registeredAt: Instant,
)

fun NotificationDeviceTokenRegistration.toResponse() = NotificationDeviceTokenResponse(id, platform.name, registeredAt)
