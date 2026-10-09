package com.buyeong.umji.api.auth.dto

import java.util.UUID

data class AuthenticatedAccountDto(val id: UUID, val name: String, val status: String)