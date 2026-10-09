package com.buyeong.umji.api.domain.account.integration.http

import com.fasterxml.jackson.annotation.JsonProperty

data class NationalTaxBusinessStatusItem(
    @JsonProperty("b_no") val businessRegistrationNumber: String? = null,
    @JsonProperty("b_stt_cd") val statusCode: String? = null,
)