package com.buyeong.umji.api.context.test

import com.buyeong.umji.api.context.FixtureMonkeyFactory
import com.buyeong.umji.api.dto.TestDto
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder

/**
 * TestDto 테스트 픽스처 빌더.
 */
object TestBuilder {
    /**
     * TestDto 빌더를 반환합니다.
     *
     * 사용 예시:
     * ```kotlin
     * val testDto = TestBuilder.testDtoBuilder()
     *     .set(TestDto::id, 0)  // 새 엔티티
     *     .sample()
     * ```
     */
    fun testDtoBuilder() =
        FixtureMonkeyFactory.fixtureMonkey
            .giveMeKotlinBuilder<TestDto>()
            .setExp(TestDto::id, 0)
}