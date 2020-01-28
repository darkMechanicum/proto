
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.31"
}

group = "com.tsarev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    fun springBootModules(vararg ids: String) =
        ids.forEach { compile("org.springframework.boot", it, "2.1.5.RELEASE") }

    springBootModules(
        "spring-boot-starter-web",
        "spring-boot-starter-security",
        "spring-boot-starter-jdbc",
        "spring-boot-starter-data-jpa"
    )
    compile(files("C:/Users/Aleksandr.Tsarev/.m2/repository/com/oracle/ojdbc7/12.1.0.2/ojdbc7-12.1.0.2.jar"))
    compile("org.jetbrains.kotlin", "kotlin-reflect", "1.3.31")
    compile("io.springfox", "springfox-swagger2", "2.9.2")
    testCompile("junit", "junit", "4.12")
    compile("com.graphql-java", "graphql-java", "13.0")
    compile("com.graphql-java", "graphql-java-spring-boot-starter-webmvc", "1.0")
    compile("com.h2database", "h2", "1.4.199")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}