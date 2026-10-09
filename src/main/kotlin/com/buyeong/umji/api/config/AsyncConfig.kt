package com.buyeong.umji.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    fun taskExecutor(): TaskExecutor {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.corePoolSize = 10

        threadPoolTaskExecutor.setTaskDecorator(MdcTaskDecorator())
        return threadPoolTaskExecutor
    }
}