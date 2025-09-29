plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)  // 添加这一行
    application
}

group = "com.zealsinger.interview_butler"
version = "1.0.0"
application {
    mainClass.set("com.zealsinger.interview_butler.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.content.negotiation)  // 使用 libs.versions.toml 中定义的依赖
    implementation(libs.ktor.serialization.kotlinx.json)   // 使用 libs.versions.toml 中定义的依赖
    implementation(libs.kotlinx.serialization.json)       // 使用 libs.versions.toml 中定义的依赖
    implementation(libs.kotlin.ai.koog)
    implementation(libs.kotlin.ktor.cors)  // CORS
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    implementation("javax.mail:mail:1.4.7")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10.1")

}
