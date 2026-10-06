package com.buyeong.umji.api.order.adapter.out.persistence

import com.buyeong.umji.api.order.application.model.TaxInvoiceBuyer
import com.buyeong.umji.api.order.application.port.out.TaxInvoiceBuyerProfilePort
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaOrderTaxInvoiceBuyerProfileAdapter(
    private val buyerGroups: BuyerGroupJpaEntityService,
    private val profiles: BuyerGroupBusinessProfileRepository,
) : TaxInvoiceBuyerProfilePort {
    @Transactional(readOnly = true)
    override fun forAccount(accountPublicId: UUID): TaxInvoiceBuyer? {
        val group = buyerGroups.activeForAccountPublicId(accountPublicId) ?: return null
        val profile = profiles.findByBuyerGroup_Id(requireNotNull(group.id))
        val complete = group.groupType == BUSINESS && listOf(
            profile?.businessRegistrationNumber,
            profile?.businessName,
            profile?.representativeName,
            profile?.postalCode,
            profile?.address1,
            profile?.businessIndustry,
            profile?.businessItem,
        ).all { !it.isNullOrBlank() }
        return TaxInvoiceBuyer(
            buyerGroupId = requireNotNull(group.publicId),
            businessRegistrationNumber = profile?.businessRegistrationNumber,
            businessName = profile?.businessName,
            representativeName = profile?.representativeName,
            postalCode = profile?.postalCode,
            address1 = profile?.address1,
            address2 = profile?.address2,
            businessIndustry = profile?.businessIndustry,
            businessItem = profile?.businessItem,
            email = profile?.taxInvoiceEmail,
            complete = complete,
        )
    }

    private companion object {
        const val BUSINESS = "BUSINESS"
    }
}
