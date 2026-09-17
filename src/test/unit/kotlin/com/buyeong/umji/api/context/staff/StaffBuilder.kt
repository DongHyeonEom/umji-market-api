package com.buyeong.umji.api.context.staff

import com.buyeong.umji.api.context.FixtureMonkeyFactory
import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.enums.StaffStatusEnum
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import net.jqwik.api.Arbitraries

/**
 * StaffDto 테스트 픽스처 빌더.
 */
object StaffBuilder {

    fun staffDtoBuilder() =
        FixtureMonkeyFactory.fixtureMonkey
            .giveMeKotlinBuilder<StaffDto>()
            .setExp(StaffDto::status, Arbitraries.of(StaffStatusEnum.entries))
            .setExp(
                StaffDto::recruitStatus,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(50),
            ).setExp(StaffDto::lastname, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::firstname, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::middlename, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::nickname, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::nationalId, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(
                StaffDto::image,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(500),
            ).setExp(StaffDto::openGroup, Arbitraries.strings().alpha().ofLength(1))
            .setExp(
                StaffDto::email,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(250),
            ).setExp(
                StaffDto::homePhone,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(20),
            ).setExp(StaffDto::handPhone, Arbitraries.strings().numeric().ofMaxLength(20))
            .setExp(StaffDto::birthdate, Arbitraries.strings().numeric().ofMaxLength(20))
            .setExp(StaffDto::gender, Arbitraries.strings().alpha().ofLength(1))
            .setExp(StaffDto::nationality, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::bankAccount, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::bank, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(
                StaffDto::loginId,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(25),
            ).setExp(StaffDto::password, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(
                StaffDto::birthPlace,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(500),
            ).setExp(
                StaffDto::homepage,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(500),
            ).setExp(StaffDto::inKorea, Arbitraries.strings().alpha().ofLength(1))
            .setExp(StaffDto::educationLevel, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(
                StaffDto::note,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .whitespace()
                    .ofLength(4000),
            ).setExp(StaffDto::onlineTimeType, Arbitraries.strings().alpha().ofLength(1))
            .setExp(StaffDto::ibtTimeType, Arbitraries.strings().alpha().ofLength(1))
            .setExp(StaffDto::newLogin, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::loginBackup, Arbitraries.strings().alpha().ofMaxLength(50))
            .setExp(StaffDto::tempType, Arbitraries.strings().alpha().ofMaxLength(20))
            .setExp(StaffDto::eipEmployeeId, Arbitraries.strings().alpha().ofMaxLength(20))
            .setExp(
                StaffDto::passwordEncryption,
                Arbitraries
                    .strings()
                    .alpha()
                    .numeric()
                    .ofMaxLength(1000),
            ).setExp(StaffDto::webLoginId, Arbitraries.strings().alpha().ofMaxLength(100))
            .setExp(StaffDto::callingNumber, Arbitraries.strings().alpha().ofMaxLength(20))
            .setExp(StaffDto::samsLoginId, Arbitraries.strings().alpha().ofMaxLength(20))
            .setExp(StaffDto::workType, Arbitraries.strings().alpha().ofMaxLength(10))
}