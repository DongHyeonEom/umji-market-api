package com.buyeong.umji.api.operation.account.dto

import java.time.Instant
import java.util.UUID

data class ConsentDataDto(
    val consentType: String,
    val documentVersion: String,
    val consentMethod: String,
    val evidenceReference: String?,
    val processedBy: UUID?,
    val consentedAt: Instant,
)