package com.buyeong.umji.api.account.application.port.out

import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfile
import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfileCommand
import java.util.UUID

interface BuyerGroupTaxInvoiceProfilePort {
    fun forAccount(accountPublicId: UUID): BuyerGroupTaxInvoiceProfile?
    fun updateForAccount(accountPublicId: UUID, command: BuyerGroupTaxInvoiceProfileCommand): BuyerGroupTaxInvoiceProfile?
    fun forGroup(groupPublicId: UUID): BuyerGroupTaxInvoiceProfile?
    fun updateForGroup(groupPublicId: UUID, command: BuyerGroupTaxInvoiceProfileCommand): BuyerGroupTaxInvoiceProfile?
}
