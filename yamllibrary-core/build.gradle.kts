plugins {
    id("yamllibrary.java-conventions")
}

description = "Platform-independent YAML configuration library"

dependencies {
    api(libs.snakeyaml)
    compileOnly(libs.annotations)
    testCompileOnly(libs.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
