package com.buyeong.umji.api.config.persistence

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

@Configuration
class DefaultDataSourceConfiguration {
    @Primary
    @Bean("defaultWriteDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.write")
    fun defaultWriteDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean("defaultReadDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.read")
    fun defaultReadDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean("defaultWriteDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.write.hikari")
    fun defaultWriteDataSource(
        @Qualifier("defaultWriteDataSourceProperties") properties: DataSourceProperties,
    ): DataSource = properties.initializeDataSourceBuilder().build()

    @Bean("defaultReadDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.read.hikari")
    fun defaultReadDataSource(
        @Qualifier("defaultReadDataSourceProperties") properties: DataSourceProperties,
    ): DataSource = properties.initializeDataSourceBuilder().build()

    @Primary
    @Bean("defaultDataSource")
    fun defaultDataSource(
        @Qualifier("defaultWriteDataSource") writeDataSource: DataSource,
        @Qualifier("defaultReadDataSource") readDataSource: DataSource,
    ): DataSource =
        LazyConnectionDataSourceProxy(writeDataSource).apply {
            setReadOnlyDataSource(readDataSource)
        }
}