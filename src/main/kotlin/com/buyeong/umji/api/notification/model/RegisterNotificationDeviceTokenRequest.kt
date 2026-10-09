package com.buyeong.umji.api.notification.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterNotificationDeviceTokenRequest(
    @field:NotBlank
    @field:Schema(description = "푸시 token 제공 platform", example = "ANDROID_FCM", type = "string", required = true)
    val platform: String,

    @field:NotBlank
    @field:Size(max = 4096,)
    @field:Schema(description = "FCM token은 1~4096자, APNs device token은 64자리 16진수", example = "fcm-device-token-example", type = "string", required = true)
    val token: String,
)