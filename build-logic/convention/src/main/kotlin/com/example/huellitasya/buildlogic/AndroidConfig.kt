package com.example.huellitasya.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

internal object HuellitasConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 24
    const val TARGET_SDK = 37
    val JAVA_VERSION = JavaVersion.VERSION_11
}

/** Configuración común a módulos de aplicación y librería Android. */
internal fun CommonExtension.configureAndroidCommon() {
    compileSdk = HuellitasConfig.COMPILE_SDK
    defaultConfig.minSdk = HuellitasConfig.MIN_SDK
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    compileOptions.sourceCompatibility = HuellitasConfig.JAVA_VERSION
    compileOptions.targetCompatibility = HuellitasConfig.JAVA_VERSION
}
