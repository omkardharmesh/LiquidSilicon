import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    android {
        namespace = "com.liquidglass.sample"
        minSdk = 24
        compileSdk = 37
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm("desktop")

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "LiquidGlassSample"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":liquidglass"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        getByName("desktopTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.liquidglass.sample.MainKt"
    }
}

tasks.withType<Test>().configureEach {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
}
