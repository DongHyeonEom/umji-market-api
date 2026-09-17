package com.buyeong.umji.api.controller.staff

import com.buyeong.umji.api.service.biz.client.CoreApi
import com.buyeong.umji.api.service.biz.client.response.StaffResponse
import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class StaffRestControllerTest(
    private val mockMvc: MockMvc,
) : BehaviorSpec() {
    @MockkBean
    lateinit var coreApi: CoreApi

    init {
        Given("Staff REST Controller") {
            val fixtureMonkey = FixtureMonkey.builder().plugin(KotlinPlugin()).build()
            val staffResponse = fixtureMonkey.giveMeKotlinBuilder<StaffResponse>().set(StaffResponse::id, 2).sample()

            every { coreApi.getStaff(any()) } returns staffResponse
            When("Core API is called to get staff details") {
                Then("Staff details should be returned from Core API") {
                    mockMvc.get("/staffs/2/core") {
                        accept = MediaType.APPLICATION_JSON
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.id") { value(2) }
                    }
                }
            }
        }
    }
}