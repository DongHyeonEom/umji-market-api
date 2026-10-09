package com.buyeong.umji.api.domain.access.dto

data class AdminAccessSnapshotDto(
    val roles: Set<String>,
    val permissions: Set<String>,
)