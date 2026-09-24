package com.buyeong.umji.api.config.persistence

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = [
        "com.buyeong.umji.api",
    ],
    entityManagerFactoryRef = "defaultEntityManagerFactory",
    transactionManagerRef = "defaultTransactionManager",
)
class DefaultJpaConfiguration {
    @Primary
    @Bean
    fun defaultEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("defaultDataSource") defaultJPADataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(defaultJPADataSource)
            .packages(
                "com.buyeong.umji.api",
            ).persistenceUnit("umjiMarket")
            .build()

    @Primary
    @Bean
    fun defaultTransactionManager(
        @Qualifier("defaultEntityManagerFactory") defaultEntityManagerFactory: EntityManagerFactory?,
    ): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = defaultEntityManagerFactory
        return transactionManager
    }
}