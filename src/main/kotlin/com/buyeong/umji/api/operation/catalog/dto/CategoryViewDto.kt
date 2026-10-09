package com.buyeong.umji.api.operation.catalog.dto

import java.util.UUID

data class CategoryViewDto(val id: UUID, val parentId: UUID?, val name: String, val path: String, val depth: Int, val displayOrder: Int, val displayStatus: String)