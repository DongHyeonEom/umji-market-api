package com.buyeong.umji.api.account.adapter.out.http

import com.buyeong.umji.api.account.application.port.out.BusinessRegistrationStatusPort
import com.buyeong.umji.api.exception.ApiCallException
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@ConfigurationProperties("umji.business-registration.status")
data class BusinessRegistrationStatusProperties(
    val baseUrl: String = "https://api.odcloud.kr/api",
    val serviceKey: String = "",
)

@Component
class NationalTaxServiceBusinessRegistrationAdapter(
    private val properties: BusinessRegistrationStatusProperties,
    builder: RestClient.Builder,
) : BusinessRegistrationStatusPort {
    private val client = builder.baseUrl(properties.baseUrl).build()

    override fun ensureNotClosed(businessRegistrationNumber: String) {
        if (properties.serviceKey.isBlank()) {
            throw ApiCallException("사업자등록 상태 확인 서비스가 설정되지 않았습니다.")
        }
        val response = try {
            client.post()
                .uri("/nts-businessman/v1/status?serviceKey={serviceKey}&returnType=JSON", properties.serviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("b_no" to listOf(businessRegistrationNumber)))
                .retrieve()
                .body(NationalTaxBusinessStatusResponse::class.java)
        } catch (ex: RestClientException) {
            throw ApiCallException("국세청 사업자등록 상태 확인에 실패했습니다.", org.springframework.http.HttpStatus.BAD_GATEWAY, ex)
        }

        if (response?.statusCode != "OK") throw ApiCallException("국세청 사업자등록 상태 응답을 확인할 수 없습니다.")
        val item = response.data.singleOrNull()?.takeIf { it.businessRegistrationNumber == businessRegistrationNumber }
            ?: throw ClientBadRequestException("사업자등록번호 상태를 확인할 수 없습니다.")
        when (item.statusCode) {
            "03" -> throw ClientBadRequestException("폐업한 사업자등록번호는 사업자 그룹으로 등록할 수 없습니다.")
            "01", "02" -> Unit
            else -> throw ClientBadRequestException("유효한 사업자등록번호를 확인할 수 없습니다.")
        }
    }
}

data class NationalTaxBusinessStatusResponse(
    @JsonProperty("status_code") val statusCode: String? = null,
    @JsonProperty("data") val data: List<NationalTaxBusinessStatusItem> = emptyList(),
)

data class NationalTaxBusinessStatusItem(
    @JsonProperty("b_no") val businessRegistrationNumber: String? = null,
    @JsonProperty("b_stt_cd") val statusCode: String? = null,
)
