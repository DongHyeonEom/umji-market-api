package com.buyeong.umji.api.order.adapter.`in`.web

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object CarrierTrackingLink {
    fun create(carrierCode: String?, trackingNumber: String?): String? {
        if (carrierCode == null || trackingNumber.isNullOrBlank()) return null
        val baseUrl = when (carrierCode.trim().uppercase()) {
            "DAESIN", "대신택배" -> "https://www.ds3211.co.kr/freight/internalFreightSearch.ht?billno="
            "KDEXP", "KD", "경동택배" -> "https://kdexp.com/newDeliverySearch.kd?barcode="
            "CHUNIL", "천일택배" -> "https://www.chunil.co.kr/HTrace/HTrace.jsp?transNo="
            else -> return null
        }
        return baseUrl + URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8)
    }
}