package com.buyeong.umji.api.auth.adapter.`in`.security

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.auth.application.port.out.AccountAuthenticationPort
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CurrentAccountSecurityAdapter(private val accounts: AccountAuthenticationPort) : CurrentAccountPort {
    override fun activeAccountPublicId(): UUID {
        val jwt = SecurityContextHolder.getContext().authentication?.principal as? Jwt
            ?: throw ClientBadRequestException("인증된 계정이 필요합니다.")
        val accountId = runCatching { UUID.fromString(jwt.subject) }
            .getOrElse { throw ClientBadRequestException("유효하지 않은 계정 토큰입니다.") }
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        require(account.status == ACTIVE) { "활성 계정만 이용할 수 있습니다." }
        return account.id
    }

    private companion object {
        const val ACTIVE = "ACTIVE"
    }
}