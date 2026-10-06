package com.buyeong.umji.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class UmjiMarketApiApplication

fun main(args: Array<String>) {
    runApplication<UmjiMarketApiApplication>(*args)
}
