plugins {
    kotlin("jvm") version "2.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.pinterest.ktlint:ktlint-rule-engine-core:1.5.0")
    compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.5.0")
}

kotlin {
    jvmToolchain(21)
}
