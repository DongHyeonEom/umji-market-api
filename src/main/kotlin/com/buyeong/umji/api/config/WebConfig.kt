package com.buyeong.umji.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.format.DateTimeFormatter

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/swagger-ui/").setViewName("forward:/swagger-ui/index.html")
    }

    override fun addFormatters(registry: org.springframework.format.FormatterRegistry) {
        val registrar = DateTimeFormatterRegistrar()
        registrar.setDateFormatter(DateTimeFormatter.ISO_DATE)
        registrar.setDateTimeFormatter(DateTimeFormatter.ISO_DATE_TIME)
        registrar.registerFormatters(registry)
    }
}
