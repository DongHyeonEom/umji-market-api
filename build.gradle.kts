@file:Suppress("UnstableApiUsage")

plugins {
    val kotlinVersion = "1.9.24"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    idea
    war

    id("org.springframework.boot") version "3.4.13"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
}

group = "com.buyeong.umji.api"

version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

ktlint {
    version.set("1.7.1")
}

// Version constants
val azureApplicationinsightsVersion = "3.7.6"
val springCloudAzureVersion = "5.21.0"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("com.azure.spring:spring-cloud-azure-starter")
    implementation("com.azure.spring:spring-cloud-azure-starter-actuator")
    implementation("com.azure.spring:spring-cloud-azure-starter-jdbc-mysql")
    implementation("com.azure.spring:spring-cloud-azure-starter-storage")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:1.12.1")
    implementation("com.github.gavlyukovskiy:flexy-pool-spring-boot-starter:1.12.1")
    implementation("com.github.loki4j:loki-logback-appender:2.0.1")
    implementation("com.microsoft.azure:applicationinsights-core:$azureApplicationinsightsVersion")
    implementation("com.microsoft.azure:applicationinsights-runtime-attach:$azureApplicationinsightsVersion")

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.persistence:jakarta.persistence-api")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.json:json:20250517")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.data:spring-data-envers")

    implementation(platform("com.azure.spring:spring-cloud-azure-dependencies:$springCloudAzureVersion"))

    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("com.navercorp.fixturemonkey:fixture-monkey-starter-kotlin:1.1.15")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
    testImplementation("io.mockk:mockk:1.14.7")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.2"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xjvm-default=all")
    }
}

// Unit Test Source Set
sourceSets {
    test {
        kotlin.srcDir("src/test/unit/kotlin")
        resources.srcDir("src/test/unit/resources")
    }
}

// Integration Test Source Set
sourceSets.register("integrationTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    kotlin.srcDir("src/test/integration/kotlin")
    resources.srcDir("src/test/integration/resources")
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

// E2E Test Source Set
sourceSets.register("e2eTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    kotlin.srcDir("src/test/e2e/kotlin")
    resources.srcDir("src/test/e2e/resources")
}

val e2eTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val e2eTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// Integration Test Resources - handle duplicates
tasks.named<Copy>("processIntegrationTestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Integration Test Task
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")

    shouldRunAfter(tasks.test)

    // build 태스크 실행 시에는 스킵
//    onlyIf { !gradle.startParameter.taskNames.any { it.contains("build") } }
}

// E2E Test Resources - handle duplicates
tasks.named<Copy>("processE2eTestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// E2E Test Task
val e2eTest by tasks.registering(Test::class) {
    description = "Runs E2E tests"
    group = "verification"
    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")

    shouldRunAfter(integrationTest)

    // build 태스크 실행 시에는 스킵
//    onlyIf { !gradle.startParameter.taskNames.any { it.contains("build") } }
}

tasks.check {
    dependsOn(integrationTest)
    dependsOn(e2eTest)
}
