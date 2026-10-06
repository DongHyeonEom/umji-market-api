package com.buyeong.umji.api.notification.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.service.NotificationDeviceTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications/device-tokens")
@Validated
@Tag(name = "알림 기기 token", description = "인증 계정의 FCM·APNs 기기 token 관리 API")
class NotificationDeviceTokenController(
    private val currentAccounts: CurrentAccountService,
    private val tokens: NotificationDeviceTokenService,
) {
    @Operation(summary = "기기 token 등록·갱신", description = "현재 활성 계정의 기기 push token을 등록하거나 동일 token을 멱등 갱신")
    @PostMapping
    fun register(@Valid @RequestBody request: RegisterNotificationDeviceTokenRequest): NotificationDeviceTokenResponse {
        val platform = NotificationDevicePlatform.entries.firstOrNull { it.name == request.platform }
            ?: throw ClientBadRequestException("지원하지 않는 push token platform입니다.")
        return tokens.register(currentAccounts.activeAccountPublicId(), platform, request.token).toResponse()
    }

    @Operation(summary = "기기 token 해제", description = "현재 계정 소유의 공개 token ID에 해당하는 기기 push token을 비활성화")
    @DeleteMapping("/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @Parameter(description = "해제할 기기 token 공개 식별자(UUID)") @PathVariable tokenId: UUID,
    ) = tokens.revoke(currentAccounts.activeAccountPublicId(), tokenId)
}
