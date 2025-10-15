plugins {
    `java-library`
    jacoco
    `maven-publish`
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.jreleaser)
}

group = "com.github.akunzai"
version = "3.1.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.sendgrid.java)
    implementation(libs.jakarta.mail.api)
    implementation(libs.log4j.core)
    annotationProcessor(libs.log4j.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.disruptor) // required for log4j2 AsyncLogger
    testImplementation(libs.javax.mail) // required for log4j2 SMTPAppender
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    spotbugs(libs.spotbugs.plugin)
    spotbugsPlugins(libs.findsecbugs.plugin)

    constraints {
        implementation(libs.jackson.databind) {
            because("previous versions have known security vulnerabilities")
        }
        implementation(libs.commons.codec) {
            because("previous versions have known security vulnerabilities")
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    if (name.contains("Test")) {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

spotbugs {
    excludeFilter.set(file("$projectDir/spotbugs-exclude.xml"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("log4j2-sendgrid-appender")
                description.set("Send log4j2 errors via SendGrid service")
                url.set("https://github.com/akunzai/log4j2-sendgrid-appender")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("akunzai")
                        name.set("Charley Wu")
                        email.set("akunzai@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/akunzai/log4j2-sendgrid-appender.git")
                }
            }
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    project {
        description.set("Send log4j2 errors via SendGrid service")
        copyright.set("2025 Charley Wu")
        authors.add("Charley Wu")
        license.set("Apache-2.0")
        inceptionYear.set("2025")
    }
    signing {
        setActive("ALWAYS")
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("RELEASE")
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.path)
                }
            }
            nexus2 {
                create("maven-central-snapshots") {
                    setActive("SNAPSHOT")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                    applyMavenCentralRules.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.path)
                }
            }
        }
    }
    release {
        github {
            overwrite.set(true)
            changelog {
                enabled.set(false)
            }
            releaseNotes {
                enabled.set(true)
                configurationFile.set(".github/release.yml")
            }
        }
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

tasks.register<JavaExec>("runTestRunner") {
    group = "Verification"
    description = "Run the test runner"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.github.akunzai.log4j.SendGridTestRunner")
    systemProperties =
        mapOf("log4j2.contextSelector" to "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
}