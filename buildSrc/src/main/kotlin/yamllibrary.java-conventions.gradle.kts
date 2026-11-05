plugins {
    `java-library`
    `maven-publish`
}

group = "fr.robie"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                // project.description is set by each module's build file; it is
                // resolved lazily because Gradle sets it after this plugin applies.
                name = provider { "${project.group}:${project.name}" }
                description = provider { project.description ?: "YAML configuration library" }
                url = "https://github.com/1robie/YamlLibrary"

                developers {
                    developer {
                        id = "1robie"
                        name = "1robie"
                        url = "https://github.com/1robie"
                    }
                }

                scm {
                    url = "https://github.com/1robie/YamlLibrary"
                    connection = "scm:git:https://github.com/1robie/YamlLibrary.git"
                    developerConnection = "scm:git:ssh://git@github.com/1robie/YamlLibrary.git"
                }

                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/1robie/YamlLibrary/issues"
                }

                licenses {
                    license {
                        name = "GNU General Public License v3.0 only"
                        url = "https://www.gnu.org/licenses/gpl-3.0.txt"
                        distribution = "repo"
                    }
                }
            }
        }
    }
    // TODO: configure a publishing target (repositories { maven { url = ... } })
}
