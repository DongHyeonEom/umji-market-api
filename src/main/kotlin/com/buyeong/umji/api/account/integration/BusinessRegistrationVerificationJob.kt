package com.buyeong.umji.api.account.integration

import com.buyeong.umji.api.account.model.BusinessRegistrationStatus
import com.buyeong.umji.api.account.integration.http.BusinessRegistrationStatusClient
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupBusinessProfileRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class BusinessRegistrationVerificationJob(
    private val profiles: BuyerGroupBusinessProfileRepository,
    private val registrationStatus: BusinessRegistrationStatusClient,
) {
    @Scheduled(fixedDelayString = "\${umji.business-registration.status.worker-delay-ms:60000}")
    fun verifyPendingProfiles() {
        val queuedProfiles = profiles.findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc(PENDING) +
            profiles.findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc(ERROR) +
            profiles.findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc(UNKNOWN)
        queuedProfiles.forEach { profile ->
            try {
                val status = registrationStatus.lookup(profile.businessRegistrationNumber.orEmpty())
                persist(profile.id!!, status.name, status != BusinessRegistrationStatus.UNKNOWN)
            } catch (_: Exception) {
                persist(profile.id!!, ERROR, false)
            }
        }
    }

    @Transactional
    fun persist(profileId: Long, status: String, verified: Boolean) {
        val profile = profiles.findById(profileId).orElse(null) ?: return
        profile.businessRegistrationVerificationStatus = status
        profile.businessRegistrationVerifiedAt = if (verified) Instant.now() else null
        profiles.save(profile)
    }

    private companion object {
        const val PENDING = "PENDING"
        const val ERROR = "ERROR"
        const val UNKNOWN = "UNKNOWN"
    }
}
