package com.buyeong.umji.api.config.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "umji.client.destinations")
class ClientDestinations(
    val core: ClientInfo,
) {
    class ClientInfo(
        val url: String,
    )
}