package com.buyeong.umji.api.domain.inventory.model

import jakarta.validation.constraints.Min

data class SetInventoryCostLotRequest(@field:Min(0) val totalPurchaseCost: Long)
