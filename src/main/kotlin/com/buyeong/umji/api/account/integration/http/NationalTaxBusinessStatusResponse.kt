package com.buyeong.umji.api.account.integration.http

import com.fasterxml.jackson.annotation.JsonProperty

data class NationalTaxBusinessStatusResponse(
    @JsonProperty("status_code") val statusCode: String? = null,
    @JsonProperty("data") val data: List<NationalTaxBusinessStatusItem> = emptyList(),
)