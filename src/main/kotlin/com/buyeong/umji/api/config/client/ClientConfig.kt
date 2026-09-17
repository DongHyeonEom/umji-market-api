package com.buyeong.umji.api.config.client

import com.buyeong.umji.api.service.biz.client.CoreApi
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Configuration
class ClientConfig(
    private val clients: ClientDestinations,
    private val properties: ClientProperties,
) {
    private fun webClientBuilder(): WebClient.Builder {
        val httpClient =
            HttpClient
                .create(connectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.httpClient.connectionTimeout)
                .responseTimeout(Duration.ofSeconds(properties.httpClient.connectionTimeout.toLong()))
                .doOnConnected { connection ->
                    connection.addHandlerLast(ReadTimeoutHandler(properties.httpClient.connectionTimeout))
                    connection.addHandlerLast(WriteTimeoutHandler(properties.httpClient.connectionTimeout))
                }

        return WebClient
            .builder()
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(properties.webClient.memorySize)
            }.clientConnector(ReactorClientHttpConnector(httpClient))
    }

    private fun connectionProvider() =
        ConnectionProvider
            .builder("umji-provider")
            .maxConnections(properties.connection.maxConnections)
            .maxIdleTime(Duration.ofSeconds(properties.connection.maxIdleTime.toLong()))
            .maxLifeTime(Duration.ofSeconds(properties.connection.maxLifeTime.toLong()))
            .pendingAcquireTimeout(Duration.ofSeconds(properties.connection.pendingAcquireTimeout.toLong()))
            .pendingAcquireMaxCount(properties.connection.pendingAcquireMaxCount)
            .evictInBackground(Duration.ofSeconds(properties.connection.evictInBackground.toLong()))
            .lifo()
            .build()

    @Bean
    fun coreApi(): CoreApi {
        val webClient = webClientBuilder().baseUrl(clients.core.url).build()
        val adapter = WebClientAdapter.create(webClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(CoreApi::class.java)
    }
}