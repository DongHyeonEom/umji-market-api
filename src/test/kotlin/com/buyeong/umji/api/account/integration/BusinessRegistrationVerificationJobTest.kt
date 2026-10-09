package com.buyeong.umji.api.account.integration

import com.buyeong.umji.api.account.integration.http.BusinessRegistrationStatusClient
import com.buyeong.umji.api.account.model.BusinessRegistrationStatus
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationBusinessProfileRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.Optional

class BusinessRegistrationVerificationJobTest {
    private val profiles = Mockito.mock(OrganizationBusinessProfileRepository::class.java)
    private val registrationStatus = Mockito.mock(BusinessRegistrationStatusClient::class.java)

    @Test
    fun `pending profile is updated after an active status response`() {
        val profile = pendingProfile()
        `when`(profiles.findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc("PENDING"))
            .thenReturn(listOf(profile))
        `when`(registrationStatus.lookup("1234567890")).thenReturn(BusinessRegistrationStatus.ACTIVE)
        `when`(profiles.findById(1L)).thenReturn(Optional.of(profile))

        BusinessRegistrationVerificationJob(profiles, registrationStatus).verifyPendingProfiles()

        assertThat(profile.businessRegistrationVerificationStatus).isEqualTo("ACTIVE")
        assertThat(profile.businessRegistrationVerifiedAt).isNotNull()
    }

    @Test
    fun `lookup failure marks profile as error without verified timestamp`() {
        val profile = pendingProfile()
        `when`(profiles.findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc("PENDING"))
            .thenReturn(listOf(profile), emptyList())
        `when`(profiles.findTop100ByBusinessRegistrationVerificationStatusOrderByIdAsc("ERROR"))
            .thenReturn(emptyList(), listOf(profile))
        `when`(registrationStatus.lookup("1234567890"))
            .thenThrow(IllegalStateException("unavailable"))
            .thenReturn(BusinessRegistrationStatus.ACTIVE)
        `when`(profiles.findById(1L)).thenReturn(Optional.of(profile))

        val job = BusinessRegistrationVerificationJob(profiles, registrationStatus)
        job.verifyPendingProfiles()

        assertThat(profile.businessRegistrationVerificationStatus).isEqualTo("ERROR")
        assertThat(profile.businessRegistrationVerifiedAt).isNull()

        job.verifyPendingProfiles()

        assertThat(profile.businessRegistrationVerificationStatus).isEqualTo("ACTIVE")
        assertThat(profile.businessRegistrationVerifiedAt).isNotNull()
    }

    private fun pendingProfile() = OrganizationBusinessProfileEntity().apply {
        id = 1L
        businessRegistrationNumber = "1234567890"
    }
}