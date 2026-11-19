// Archivo de build raíz.
//
// AGP 9 trae Kotlin integrado ("built-in Kotlin") y depende de una versión mínima
// del Kotlin Gradle Plugin (KGP). Para usar una versión más nueva, la guía oficial
// pide declararla en el classpath del buildscript raíz:
// https://developer.android.com/build/releases/agp-9-0-0-release-notes
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}
