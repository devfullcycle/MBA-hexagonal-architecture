plugins {
    `java-conventions`
    `java-library`
}

group = "br.com.fullcycle.application"

dependencies {
    implementation(project(":domain"))
}