plugins {
    `java-library`
    jacoco
    `maven-publish`
    signing
    alias(libs.plugins.spotbugs)
}

group = "com.github.akunzai"
version = "3.1.0"

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
            name = "OSSRH"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = project.findProperty("sonatype.user")?.toString() ?: System.getenv("SONATYPE_USER")
                password = project.findProperty("sonatype.token")?.toString() ?: System.getenv("SONATYPE_TOKEN")
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
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