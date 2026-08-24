plugins {
    java
    `java-conventions`
    `jacoco-report-aggregation`
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
}

group = "br.com.fullcycle.infrastructure"

tasks.bootJar {
    archiveBaseName.set("application")
    destinationDirectory.set(file("${rootProject.buildDir}/libs"))
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation("io.hypersistence:hypersistence-tsid:2.1.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.graphql:spring-graphql-test")

    testRuntimeOnly("com.h2database:h2")
}

tasks.testCodeCoverageReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("$rootDir/build/reports/jacoco/test/jacocoTestReport.xml"))

        html.required.set(true)
        html.outputLocation.set(file("$rootDir/build/reports/jacoco/test/"))
    }
}

tasks.named("jacocoTestReport") {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}