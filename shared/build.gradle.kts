import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)

}

kotlin {
    jvm()

    androidLibrary {
       namespace = "org.example.project.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation("io.ktor:ktor-client-android:3.4.3")      // движок для Android [citation:9]
            implementation("io.ktor:ktor-client-okhttp:3.4.3")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
        }
        commonMain.dependencies {
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("io.ktor:ktor-client-core:3.4.3")
            implementation("io.ktor:ktor-client-content-negotiation:3.4.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.6.10")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:3.4.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
compose.desktop {
    application {

        mainClass = "MainKt"

        nativeDistributions {

            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )

            packageName = "HotelRS"
            packageVersion = "1.0.0"

            windows {

                iconFile.set(
                    project.file(
                        "src/jvmMain/resources/logo.ico"
                    )
                )
            }
        }
    }
}