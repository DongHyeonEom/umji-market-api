package com.buyeong.umji.api.operation.catalog.dto

import java.util.UUID

data class CategoryCommandDto(val name: String, val parentId: UUID?, val displayOrder: Int, val displayStatus: String)