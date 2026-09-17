package com.buyeong.umji.api.service.example

import com.buyeong.umji.api.dto.example.AsyncProxyResultDto
import com.buyeong.umji.api.dto.example.InternalUserRequestDto
import com.buyeong.umji.api.dto.example.ProxyRequestStatusDto
import com.buyeong.umji.api.enums.example.RequestStatus
import com.buyeong.umji.api.util.logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 비동기 프록시 서비스 예제 (상태 추적 기능 포함).
 *
 * 비동기 요청의 상태를 추적하고 나중에 결과를 조회할 수 있습니다.
 *
 * 주요 기능:
 * - 비동기 요청 접수 및 즉시 requestId 반환
 * - 백그라운드에서 외부 API 호출
 * - 요청 상태 추적 및 조회
 * - 재시도 횟수 관리
 *
 * 사용 예시:
 * ```kotlin
 * // 1. 비동기 요청 접수
 * val requestId = asyncProxyService.submitRequest(request)
 *
 * // 2. 상태 조회
 * val status = asyncProxyService.getRequestStatus(requestId)
 *
 * // 3. 결과 조회 (완료된 경우)
 * val result = asyncProxyService.getResult(requestId)
 * ```
 *
 * 프로덕션 환경에서는 상태를 데이터베이스에 저장하도록 구현을 변경해야 합니다.
 */
@Service
class ExampleAsyncProxyService(
    private val exampleWebClientService: ExampleWebClientService,
) {
    companion object {
        private val logger = logger()
    }

    /**
     * 요청 상태 저장소.
     * 프로덕션 환경에서는 데이터베이스로 교체 필요.
     */
    private val requestStatusMap = ConcurrentHashMap<String, ProxyRequestStatusDto>()

    /**
     * 결과 저장소.
     * 프로덕션 환경에서는 데이터베이스로 교체 필요.
     */
    private val resultMap = ConcurrentHashMap<String, AsyncProxyResultDto>()

    /**
     * 비동기 요청 접수.
     *
     * 요청을 접수하고 즉시 requestId를 반환합니다.
     * 실제 처리는 백그라운드에서 진행됩니다.
     *
     * @param request 내부 시스템 요청
     * @return 요청 추적용 ID
     */
    fun submitRequest(request: InternalUserRequestDto): String {
        // 상태 초기화
        val status =
            ProxyRequestStatusDto(
                userId = request.userId,
                status = RequestStatus.PENDING,
                message = "요청이 접수되었습니다.",
            )

        requestStatusMap[status.requestId] = status
        logger.info("비동기 요청 접수: requestId=${status.requestId}, userId=${request.userId}")

        // 백그라운드 처리 시작
        processAsync(status.requestId, request)

        return status.requestId
    }

    /**
     * 요청 상태 조회.
     *
     * @param requestId 요청 ID
     * @return 요청 상태 (없으면 null)
     */
    fun getRequestStatus(requestId: String): ProxyRequestStatusDto? = requestStatusMap[requestId]

    /**
     * 요청 결과 조회.
     *
     * @param requestId 요청 ID
     * @return 요청 결과 (처리 완료 전이면 null)
     */
    fun getResult(requestId: String): AsyncProxyResultDto? = resultMap[requestId]

    /**
     * 요청 취소.
     *
     * 대기 중인 요청만 취소 가능합니다.
     *
     * @param requestId 요청 ID
     * @return 취소 성공 여부
     */
    fun cancelRequest(requestId: String): Boolean {
        val status = requestStatusMap[requestId] ?: return false

        if (status.status != RequestStatus.PENDING) {
            logger.warn("취소 불가: requestId=$requestId, 현재 상태=${status.status}")
            return false
        }

        updateStatus(requestId, RequestStatus.CANCELLED, "요청이 취소되었습니다.")
        return true
    }

    /**
     * 백그라운드 처리.
     *
     * @Async 어노테이션으로 별도 스레드에서 실행됩니다.
     */
    @Async
    fun processAsync(
        requestId: String,
        request: InternalUserRequestDto,
    ) {
        logger.info("백그라운드 처리 시작: requestId=$requestId")

        // 취소된 요청인지 확인
        val currentStatus = requestStatusMap[requestId]
        if (currentStatus?.status == RequestStatus.CANCELLED) {
            logger.info("요청이 취소됨: requestId=$requestId")
            return
        }

        // 상태를 처리 중으로 업데이트
        updateStatus(requestId, RequestStatus.PROCESSING, "처리 중입니다.")

        try {
            // 외부 API 호출
            val response = exampleWebClientService.registerUser(request)

            // 성공 처리
            if (response.success) {
                updateStatus(requestId, RequestStatus.COMPLETED, "처리가 완료되었습니다.")
                resultMap[requestId] =
                    AsyncProxyResultDto(
                        requestId = requestId,
                        success = true,
                        status = RequestStatus.COMPLETED,
                        response = response,
                    )
            } else {
                // 비즈니스 실패
                updateStatus(requestId, RequestStatus.FAILED, response.message)
                resultMap[requestId] =
                    AsyncProxyResultDto(
                        requestId = requestId,
                        success = false,
                        status = RequestStatus.FAILED,
                        response = response,
                        message = response.message,
                    )
            }

            logger.info("백그라운드 처리 완료: requestId=$requestId, success=${response.success}")
        } catch (e: Exception) {
            // 시스템 에러
            logger.error("백그라운드 처리 실패: requestId=$requestId", e)

            val retryCount = incrementRetryCount(requestId)
            val finalStatus = if (retryCount >= 3) RequestStatus.EXHAUSTED else RequestStatus.FAILED

            updateStatus(
                requestId = requestId,
                status = finalStatus,
                message = "처리 중 오류가 발생했습니다: ${e.message}",
                errorDetails = e.stackTraceToString(),
            )

            resultMap[requestId] =
                AsyncProxyResultDto(
                    requestId = requestId,
                    success = false,
                    status = finalStatus,
                    message = e.message,
                )
        }
    }

    /**
     * 상태 업데이트.
     */
    private fun updateStatus(
        requestId: String,
        status: RequestStatus,
        message: String,
        errorDetails: String? = null,
    ) {
        requestStatusMap.computeIfPresent(requestId) { _, current ->
            current.copy(
                status = status,
                message = message,
                updatedAt = LocalDateTime.now(),
                completedAt = if (status.isTerminal()) LocalDateTime.now() else null,
                errorDetails = errorDetails,
            )
        }
    }

    /**
     * 재시도 횟수 증가.
     */
    private fun incrementRetryCount(requestId: String): Int {
        var newCount = 0
        requestStatusMap.computeIfPresent(requestId) { _, current ->
            newCount = current.retryCount + 1
            current.copy(retryCount = newCount, updatedAt = LocalDateTime.now())
        }
        return newCount
    }

    /**
     * 종료 상태 여부 확인.
     */
    private fun RequestStatus.isTerminal(): Boolean =
        this in listOf(RequestStatus.COMPLETED, RequestStatus.EXHAUSTED, RequestStatus.CANCELLED)
}