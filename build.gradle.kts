// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.androidx.room) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

/**
 * The gate every change has to pass. Needs no device, so it is the one to run constantly.
 *
 * `test` and `testDebugUnitTest` are both listed on purpose: an Android module has only
 * the latter, a pure-Kotlin module only the former, so naming one of them would silently
 * skip the other's tests the day this project grows a second module.
 */
tasks.register("verify") {
    group = "verification"
    description = "Assembles the debug APK and runs every unit test. No device needed."
    dependsOn(":app:assembleDebug", ":app:testDebugUnitTest", ":app:test")
}

/**
 * The gate before a PR. Adds the instrumented tests, which need a connected device or a
 * running emulator — and which are the only coverage of the things that can destroy user
 * data: the favorite toggle's transaction, MIGRATION_2_3, and the v1 upgrade path. None of
 * those can be checked off-device, so an instrumented suite nobody runs is no suite at all.
 */
tasks.register("verifyOnDevice") {
    group = "verification"
    description = "Everything in `verify`, plus the instrumented tests. Needs a device."
    dependsOn("verify", ":app:connectedDebugAndroidTest")
}
