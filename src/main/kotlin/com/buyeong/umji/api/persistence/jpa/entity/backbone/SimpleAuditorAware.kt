package com.buyeong.umji.api.persistence.jpa.entity.backbone

import com.buyeong.umji.api.constant.Constant
import org.springframework.data.domain.AuditorAware
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Optional

/**
 * 요청 헤더에서 사용자 ID를 추출하는 AuditorAware 구현체.
 *
 * - X-User-Id 헤더가 있으면 해당 값 사용
 * - 헤더가 없거나 파싱 실패 시 기본값(SYSTEM_USER_ID) 사용
 *
 * Spring Security 사용 시 아래와 같이 확장 가능:
 * ```kotlin
 * class SpringSecurityAuditorAware : AuditorAware<Int> {
 *     override fun getCurrentAuditor(): Optional<Int> =
 *         Optional.ofNullable(SecurityContextHolder.getContext())
 *             .map(SecurityContext::getAuthentication)
 *             .filter(Authentication::isAuthenticated)
 *             .map { auth -> (auth.principal as UserDetails).userId }
 * }
 * ```
 */
class SimpleAuditorAware : AuditorAware<Int> {
    override fun getCurrentAuditor(): Optional<Int> {
        val userId = getUserIdFromRequest() ?: Constant.SYSTEM_USER_ID
        return Optional.of(userId)
    }

    private fun getUserIdFromRequest(): Int? =
        runCatching {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            requestAttributes?.request?.getHeader(Constant.HEADER_USER_ID)?.toInt()
        }.getOrNull()
}