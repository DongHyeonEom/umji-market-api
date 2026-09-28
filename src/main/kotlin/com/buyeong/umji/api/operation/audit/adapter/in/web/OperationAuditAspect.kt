package com.buyeong.umji.api.operation.audit.adapter.`in`.web

import com.buyeong.umji.api.constant.Constant
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditEvent
import com.buyeong.umji.api.operation.audit.application.port.`in`.OperationAuditUseCase
import com.buyeong.umji.api.operation.model.OperationAccountResponse
import com.buyeong.umji.api.operation.model.OperationCatalogResourceResponse
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping
import java.time.Instant
import java.util.UUID

@Aspect
@Component
class OperationAuditAspect(
    private val audit: OperationAuditUseCase,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
    }

    @Around("execution(* com.buyeong.umji.api.operation..adapter.in.web.Operation*Controller.*(..))")
    fun recordSuccessfulChanges(joinPoint: ProceedingJoinPoint): Any? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        if (request == null || request.method !in MUTATING_METHODS) return joinPoint.proceed()

        return transaction.execute {
            val response = joinPoint.proceed()
            audit.record(request.toAuditEvent(response))
            response
        }
    }

    private fun jakarta.servlet.http.HttpServletRequest.toAuditEvent(response: Any?): OperationAuditEvent {
        val route = getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)?.toString() ?: requestURI
        val variables = getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *> ?: emptyMap<Any, Any>()
        val resourceType = route.removePrefix("/api/operation/").substringBefore('/').let { segment ->
            when {
                segment == "accounts" && route.contains("/roles") -> "ACCOUNT_ROLE"
                segment == "accounts" -> "ACCOUNT"
                segment == "categories" -> "CATEGORY"
                segment == "brands" -> "BRAND"
                segment == "products" -> "PRODUCT"
                segment == "inventory" -> "INVENTORY"
                segment == "orders" -> "ORDER"
                else -> "OPERATION"
            }
        }
        val routeId = variables.entries.firstNotNullOfOrNull { (_, value) -> value?.toString()?.let(::uuidOrNull) }
        val resultId = when (response) {
            is OperationAccountResponse -> response.id
            is OperationCatalogResourceResponse -> response.id
            is InventoryStockResponse -> response.skuId
            else -> null
        }
        val roleCode = variables["roleCode"]?.toString()?.takeIf { it in MANAGED_ROLE_CODES }
        val action = buildString {
            append(method)
            append(' ')
            append(route)
            roleCode?.let { append(" [").append(it).append(']') }
        }
        val actorId = (SecurityContextHolder.getContext().authentication?.principal as? Jwt)?.subject?.let(::uuidOrNull)
        val traceId = MDC.get(Constant.KEY_TRACE_ID)?.takeIf { it.length <= 64 && TRACE_ID_PATTERN.matches(it) }
        return OperationAuditEvent(actorId, action, resourceType, routeId ?: resultId, traceId, Instant.now())
    }

    private fun uuidOrNull(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

    private companion object {
        val MANAGED_ROLE_CODES = setOf("PRODUCT_MANAGER", "ORDER_MANAGER", "INVENTORY_MANAGER")
        val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        val TRACE_ID_PATTERN = Regex("[A-Za-z0-9._-]{1,64}")
    }
}