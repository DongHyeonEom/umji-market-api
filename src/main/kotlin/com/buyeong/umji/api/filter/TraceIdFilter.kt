package com.buyeong.umji.api.filter

import com.buyeong.umji.api.constant.Constant
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        try {
            val httpRequest = request as? HttpServletRequest

            // X-Trace-Id 헤더가 있으면 사용, 없으면 새로 생성
            val traceId = httpRequest?.getHeader(Constant.HEADER_TRACE_ID) ?: UUID.randomUUID().toString()
            MDC.put(Constant.KEY_TRACE_ID, traceId)

            // X-User-Id 헤더가 있으면 MDC에 추가
            httpRequest?.getHeader(Constant.HEADER_USER_ID)?.let { userId ->
                MDC.put(Constant.KEY_USER_ID, userId)
            }

            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}