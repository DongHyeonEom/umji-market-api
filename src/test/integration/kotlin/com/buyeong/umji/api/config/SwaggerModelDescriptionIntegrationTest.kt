package com.buyeong.umji.api.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "springdoc.api-docs.enabled=true",
    ],
)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SwaggerModelDescriptionIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `openapi exposes controller tags operation summaries and detailed nested models`() {
        mockMvc.get("/v3/api-docs/umji-market-api")
            .andExpect {
                status { isOk() }
                jsonPath("$.paths['/api/auth/login'].post.tags[0]") { value("인증") }
                jsonPath("$.paths['/api/auth/login'].post.summary") { value("휴대폰 번호로 로그인") }
                jsonPath("$.paths['/api/auth/login'].post.description") { exists() }
                jsonPath("$.components.schemas.PhoneLoginRequest.description") { exists() }
                jsonPath("$.components.schemas.PhoneLoginRequest.properties.phone.description") { exists() }
                jsonPath("$.components.schemas.PhoneLoginRequest.properties.phone.example") { value("01012345678") }
                jsonPath("$.components.schemas.PhoneLoginRequest.properties.phone.type") { value("string") }
                jsonPath("$.components.schemas.PhoneLoginRequest.required[0]") { value("phone") }
                jsonPath("$.components.schemas.PhoneLoginRequest.properties.deviceId.description") { exists() }
                jsonPath("$.components.schemas.PhoneLoginRequest.required") { isArray() }
                jsonPath("$.components.schemas.OrderPageResponse.properties.items.items.${'$'}ref") { value("#/components/schemas/OrderResponse") }
                jsonPath("$.components.schemas.OrderPageResponse.properties.page.type") { value("integer") }
                jsonPath("$.components.schemas.OrderResponse.description") { exists() }
                jsonPath("$.components.schemas.OrderResponse.properties.totalAmount.description") { exists() }
                jsonPath("$.paths['/api/products/{productId}'].get.parameters[0].description") { exists() }
            }
    }
}