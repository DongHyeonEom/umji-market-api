package com.buyeong.umji.api.domain.operation.account.dto

import java.util.UUID

data class ConsentCommandDto(
    val consentType: String,
    val documentVersion: String,
    val consentMethod: String,
    val evidenceReference: String?,
    val processedBy: UUID,
)