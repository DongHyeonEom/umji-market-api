package com.buyeong.umji.api.domain.auth.dto

import java.util.UUID

data class WebPasswordCommandDto(val accountId: UUID, val password: String)