package com.buyeong.umji.api.operation.catalog.dto

import java.util.UUID

data class SkuCommandDto(val skuCode: String, val name: String, val salePrice: Long, val listPrice: Long?, val salesStatus: String, val optionValueIds: Set<UUID>)