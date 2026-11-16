import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    `maven-publish`
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    explicitApi()

    android {
        namespace = "io.github.omkardharmesh.liquidsilicon"
        minSdk = 24
        compileSdk = 37
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.graphics)
        }

        val skikoMain = create("skikoMain") {
            dependsOn(commonMain.get())
        }
        getByName("desktopMain").dependsOn(skikoMain)
        iosMain.get().dependsOn(skikoMain)

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("LiquidSilicon")
            description.set("iOS-style Liquid Glass for Compose Multiplatform")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
        }
    }
    repositories {
        val repoUrl = providers.gradleProperty("liquidsilicon.repoUrl").orNull
        if (repoUrl != null) {
            maven {
                name = "remote"
                url = uri(repoUrl)
                credentials {
                    username = providers.gradleProperty("liquidsilicon.repoUser").orNull
                    password = providers.gradleProperty("liquidsilicon.repoToken").orNull
                }
            }
        }
    }
}
