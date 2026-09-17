package com.buyeong.umji.api.dto.example

import com.buyeong.umji.api.enums.example.UserStatus
import java.time.LocalDateTime

/**
 * 내부 시스템 사용자 데이터 DTO.
 */
data class InternalUserDataDto(
    val userId: String,
    val registeredAt: LocalDateTime?,
    val status: UserStatus,
)