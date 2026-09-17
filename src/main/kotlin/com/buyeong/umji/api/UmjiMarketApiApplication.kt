package com.buyeong.umji.api

import com.buyeong.umji.api.config.client.ClientDestinations
import com.buyeong.umji.api.config.client.ClientProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ClientDestinations::class, ClientProperties::class)
class UmjiMarketApiApplication

fun main(args: Array<String>) {
    runApplication<UmjiMarketApiApplication>(*args)
}