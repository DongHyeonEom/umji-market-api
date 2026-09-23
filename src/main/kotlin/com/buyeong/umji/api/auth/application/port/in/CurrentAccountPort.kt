package com.buyeong.umji.api.auth.application.port.`in`

import java.util.UUID

interface CurrentAccountPort {
    fun activeAccountPublicId(): UUID
}
