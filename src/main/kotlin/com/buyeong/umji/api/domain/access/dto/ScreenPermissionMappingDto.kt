package com.buyeong.umji.api.domain.access.dto

data class ScreenPermissionMappingDto(
    val screenCode: String,
    val routeKey: String,
    val permissionMatchMode: String,
    val permissionCode: String?,
    val requiredPermissions: Set<String> = emptySet(),
)