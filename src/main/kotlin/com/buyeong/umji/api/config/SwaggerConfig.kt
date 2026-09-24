package com.buyeong.umji.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger/OpenAPI 설정.
 *
 * API 그룹은 번호로 정렬됩니다:
 * - 0. All: 전체 API
 * - 1~98: 도메인별 API 그룹
 * - 99. Actuator: 모니터링 API
 *
 * 새 도메인 추가 시 아래 예시를 참고하세요:
 * ```kotlin
 * @Bean
 * fun getDomainApi(): GroupedOpenApi =
 *     GroupedOpenApi.builder()
 *         .group("2. Domain")
 *         .packagesToScan("com.buyeong.umji.api.controller.domain")
 *         .build()
 * ```
 */
@Configuration
class SwaggerConfig {
    companion object {
        private const val BASE_PACKAGE = "com.buyeong.umji.api.controller"
    }

    @Bean
    fun getAllApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("0. All")
            .packagesToScan(BASE_PACKAGE)
            .build()

    @Bean
    fun api(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("umji-market-api")
            .pathsToMatch("/api/**")
            .build()

    // 도메인 추가 예시:
    // @Bean
    // fun getCounselingApi(): GroupedOpenApi =
    //     GroupedOpenApi.builder()
    //         .group("2. Counseling")
    //         .packagesToScan("$BASE_PACKAGE.counseling")
    //         .build()

    @Bean
    fun getActuatorApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("99. Actuator")
            .pathsToMatch("/actuator/**")
            .build()

    @Bean
    fun getOpenApi(): OpenAPI = OpenAPI().components(Components()).info(getInfo())

    private fun getInfo(): Info =
        Info()
            .version("1.0.0")
            .description("Umji Market API DOC")
            .title("Umji Market API")
}