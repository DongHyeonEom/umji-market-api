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
    version.set("1.5.0")
}

val formatSchemaProperties by tasks.registering {
    group = "formatting"
    description = "Formats Kotlin @field:Schema annotations and constructor properties."
    doLast {
        val schemaProperty = Regex("(?m)^(.*@field:Schema\\(.*\\))[ \\t]+((?:val|var)\\s+.*)$")
        val closingSchemaProperty = Regex("(?m)^(\\s*\\))([ \\t]+)((?:val|var)\\s+.*)$")
        val inlineDataClassProperty = Regex("(?m)^(\\s*data class [A-Za-z0-9_]+)\\((.*@field:Schema\\(.*\\))\\s+((?:val|var)\\s+.*)\\)$")
        val sizeAnnotation = Regex("@field:Size\\(\\s*((?:(?:min|max)\\s*=\\s*\\d+\\s*,?\\s*)+)\\)", setOf(RegexOption.DOT_MATCHES_ALL))
        val combinedAnnotations = Regex("(?m)^([ \\t]*)(@field:[^\\r\\n]*)$")

        fileTree("src") { include("**/*.kt") }.forEach { sourceFile ->
            val original = sourceFile.readText()
            val lineEnding = if ("\r\n" in original) "\r\n" else "\n"
            var formatted = sizeAnnotation.replace(original) { match ->
                val arguments = match.groupValues[1]
                    .replace(Regex("\\s*,\\s*"), ", ")
                    .replace(Regex("\\s*=\\s*"), " = ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .trimEnd(',')
                "@field:Size($arguments,)"
            }
            formatted = inlineDataClassProperty.replace(formatted) { match ->
                val classIndent = match.groupValues[1].takeWhile { it == ' ' || it == '\t' }
                val parameterIndent = "$classIndent    "
                "${match.groupValues[1]}($lineEnding$parameterIndent${match.groupValues[2]}$lineEnding$parameterIndent${match.groupValues[3]},$lineEnding$classIndent)"
            }
            formatted = schemaProperty.replace(formatted) { match ->
                "${match.groupValues[1]}$lineEnding${match.groupValues[1].takeWhile { it == ' ' || it == '\t' }}${match.groupValues[2]}"
            }
            formatted = closingSchemaProperty.replace(formatted) { match ->
                val indentation = match.groupValues[1].takeWhile { it == ' ' || it == '\t' }
                "${match.groupValues[1]}$lineEnding$indentation${match.groupValues[3]}"
            }
            formatted = combinedAnnotations.replace(formatted) { match ->
                val indentation = match.groupValues[1]
                val annotations = match.groupValues[2]
                if (annotations.count { it == '@' } < 2) {
                    match.value
                } else {
                    val parts = annotations.split(Regex("[ \\t]+(?=@field:)"))
                        .map(String::trim)
                    val packed = mutableListOf<String>()
                    var current = ""
                    parts.forEach { annotation ->
                        val candidate = if (current.isEmpty()) annotation else "$current $annotation"
                        if (indentation.length + candidate.length <= 120) {
                            current = candidate
                        } else {
                            if (current.isNotEmpty()) packed += current
                            current = annotation
                        }
                    }
                    if (current.isNotEmpty()) packed += current
                    packed.joinToString(lineEnding) { "$indentation$it" }
                }
            }
            val propertyBeforeAnnotation = Regex("(?m)^([ \\t]*(?:val|var)\\s+[^\\r\\n]*,)\\r?\\n(?=[ \\t]*@field:)")
            formatted = propertyBeforeAnnotation.replace(formatted) { match ->
                match.groupValues[1] + lineEnding + lineEnding
            }
            if (formatted != original) sourceFile.writeText(formatted)
        }
    }
}

val checkSchemaProperties by tasks.registering {
    group = "verification"
    description = "Checks Kotlin @field:Schema formatting and spacing between annotated constructor properties."
    doLast {
        val inlineSchemaProperty = Regex("@field:Schema\\(.*\\)\\s+(?:val|var)\\s+")
        val closingInlineProperty = Regex("(?m)^\\s*\\)\\s+(?:val|var)\\s+")
        val multilineNumericSize = Regex("@field:Size\\([^)]*\\r?\\n[^)]*\\)")
        val numericSizeWithoutTrailingComma = Regex("@field:Size\\((?:(?:min|max)\\s*=\\s*\\d+\\s*,\\s*)*(?:min|max)\\s*=\\s*\\d+\\)")
        val violations = fileTree("src") { include("**/*.kt") }.flatMap { sourceFile ->
            val lines = sourceFile.readLines()
            val lineViolations = lines.mapIndexedNotNull { index, line ->
                val lineNumber = index + 1
                when {
                    inlineSchemaProperty.containsMatchIn(line) -> "$sourceFile:$lineNumber: property shares a line with @field:Schema"
                    line.split("@field:").size > 2 && line.length > 120 -> "$sourceFile:$lineNumber: combined field annotations exceed 120 characters"
                    line.trimStart().matches(Regex("(?:val|var)\\s+.*,")) && lines.getOrNull(index + 1)?.trimStart()?.startsWith("@field:") == true ->
                        "$sourceFile:$lineNumber: add a blank line after the field"
                    closingInlineProperty.containsMatchIn(line) -> "$sourceFile:$lineNumber: property shares a line with the closing annotation"
                    else -> null
                }
            }
            val sizeViolations = buildList {
                if (multilineNumericSize.containsMatchIn(sourceFile.readText())) {
                    add("$sourceFile: numeric @field:Size arguments should use one line")
                }
                sourceFile.readLines().forEachIndexed { index, line ->
                    if (numericSizeWithoutTrailingComma.containsMatchIn(line)) {
                        add("$sourceFile:${index + 1}: add a trailing comma to numeric @field:Size arguments")
                    }
                }
            }
            lineViolations + sizeViolations
        }
        check(violations.isEmpty()) {
            "Format field annotations and constructor properties:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.named("ktlintCheck") { dependsOn(checkSchemaProperties) }
tasks.named("ktlintFormat") { dependsOn(formatSchemaProperties) }

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
    implementation("com.google.firebase:firebase-admin:9.11.0")

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
    implementation("org.jsoup:jsoup:1.23.2")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.data:spring-data-envers")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")

    implementation(platform("com.azure.spring:spring-cloud-azure-dependencies:$springCloudAzureVersion"))

    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

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