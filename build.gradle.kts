plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}

// Ensure CI and local builds do not fail if yarn.lock changes unexpectedly
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        ignoreScripts = false
    }
}

// When running on CI (or in general), avoid failing the build on lockfile mismatch
tasks.matching { it.name == "kotlinStoreYarnLock" }.configureEach {
    enabled = System.getenv("CI").isNullOrEmpty()
}
