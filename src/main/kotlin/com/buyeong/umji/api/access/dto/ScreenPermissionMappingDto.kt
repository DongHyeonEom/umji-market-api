package com.buyeong.umji.api.access.dto

data class ScreenPermissionMappingDto(
    val screenCode: String,
    val routeKey: String,
    val permissionMatchMode: String,
    val permissionCode: String?,
    val requiredPermissions: Set<String> = emptySet(),
)