package com.buyeong.umji.api.account.adapter.`in`.web

import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfile
import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfileCommand
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupTaxInvoiceProfileUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalBuyerGroupTaxInvoiceProfileUseCase(private val profiles: BuyerGroupTaxInvoiceProfileUseCase) {
    @Transactional(readOnly = true)
    fun forAccount(accountPublicId: UUID): BuyerGroupTaxInvoiceProfile = profiles.forAccount(accountPublicId)

    @Transactional
    fun updateForAccount(accountPublicId: UUID, command: BuyerGroupTaxInvoiceProfileCommand): BuyerGroupTaxInvoiceProfile =
        profiles.updateForAccount(accountPublicId, command)

    @Transactional(readOnly = true)
    fun forGroup(groupPublicId: UUID): BuyerGroupTaxInvoiceProfile = profiles.forGroup(groupPublicId)

    @Transactional
    fun updateForGroup(groupPublicId: UUID, command: BuyerGroupTaxInvoiceProfileCommand): BuyerGroupTaxInvoiceProfile =
        profiles.updateForGroup(groupPublicId, command)
}
