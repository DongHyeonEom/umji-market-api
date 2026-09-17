package com.buyeong.umji.api.config.persistence

import com.buyeong.umji.api.persistence.jdbc.converter.StaffStatusReadingConverter
import com.buyeong.umji.api.persistence.jdbc.converter.StaffStatusWritingConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.TransactionManager
import javax.sql.DataSource

@EnableJdbcRepositories(
    basePackages = [
        "com.buyeong.umji.api.persistence.jdbc.repository.cdi",
    ],
    transactionManagerRef = "defaultJdbcTransactionManager",
)
@Configuration
class DefaultJdbcConfiguration : AbstractJdbcConfiguration() {

    override fun jdbcCustomConversions(): JdbcCustomConversions =
        JdbcCustomConversions(
            listOf(
                StaffStatusReadingConverter(),
                StaffStatusWritingConverter(),
            ),
        )

    @Primary
    @Bean
    fun defaultJdbcTemplate(
        @Qualifier("defaultDataSource") dataSource: DataSource,
    ): JdbcTemplate = JdbcTemplate(dataSource)

    @Primary
    @Bean
    fun defaultNamedParameterJdbcOperations(
        @Qualifier("defaultDataSource") dataSource: DataSource,
    ): NamedParameterJdbcOperations = NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun defaultJdbcTransactionManager(
        @Qualifier("defaultDataSource") dataSource: DataSource,
    ): TransactionManager = DataSourceTransactionManager(dataSource)
}