plugins {
    id("yamllibrary.java-conventions")
}

description = "Paper/Bukkit integration for YamlLibrary"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":yamllibrary-core"))

    // paper-api carries Adventure (via its BOM) and Guava transitively, so they
    // are on the compile classpath without being pinned here.
    compileOnly(libs.paper.api)
    compileOnly(libs.annotations)
    testCompileOnly(libs.annotations)

    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockbukkit)
}
