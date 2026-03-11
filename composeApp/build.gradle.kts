import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.collektive)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Configure all iOS targets in one go
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeAppFramework"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.ui.tooling.preview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.bundles.collektive)
                implementation(libs.kmqtt.common)
                implementation(libs.kmqtt.client)
                implementation(libs.logging)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.material.icons.extended)
                implementation(libs.kotlinx.datetime)
                implementation(libs.geo.compose)
                implementation(libs.permissions.compose)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.play.services.location)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "it.unibo.collektive.echo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "it.unibo.collektive.echo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Exclude generated sources from detekt and CPD analysis
afterEvaluate {
    // Compose Multiplatform ships its own copies of androidx.* libraries (annotation, collection,
    // lifecycle, savedstate, runtime) which produce unavoidable "duplicate unique_name" KLIB warnings.
    // With -Werror (set by kotlin-qa) these become errors, but only in metadata compilation tasks.
    // Disable allWarningsAsErrors on those tasks only, keeping it active everywhere else.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        if (name.contains("Metadata", ignoreCase = true)) {
            compilerOptions.allWarningsAsErrors.set(false)
        }
    }

    tasks.matching { it.name.startsWith("detekt") }.configureEach {
        if (this is SourceTask) {
            exclude("**/generated/**")
        }
    }
    tasks.withType<de.aaschmid.gradle.plugins.cpd.Cpd>().configureEach {
        source = fileTree(projectDir) {
            include("src/**/*.kt")
        }
    }
}
