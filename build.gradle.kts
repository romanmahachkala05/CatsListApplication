// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

/**
 * The gate every change has to pass. Needs no device, so it is the one to run constantly.
 *
 * Depends on every subproject's own `check` — which ktlint, detekt and the unit test tasks
 * all attach themselves to by default — rather than naming module-specific task paths. An
 * Android module's unit tests are named differently from a pure-Kotlin module's, and the
 * module list itself keeps growing; `check` is the one name every module has in common.
 */
tasks.register("verify") {
    group = "verification"
    description = "Checks formatting and static analysis, assembles the debug APK, runs every unit test in every module. No device needed."
    dependsOn(":app:assembleDebug")
    // `include(":core:data")` also creates an unbuildable ":core" grouping project with no
    // build.gradle.kts of its own — filter to the subprojects that are actual modules.
    dependsOn(subprojects.filter { it.buildFile.exists() }.map { "${it.path}:check" })
}

/**
 * The gate before a PR. Adds every module's instrumented tests, which need a connected device
 * or a running emulator — and which are the only coverage of the things that can destroy user
 * data: the favorite toggle's transaction, MIGRATION_2_3, and the v1 upgrade path. None of
 * those can be checked off-device, so an instrumented suite nobody runs is no suite at all.
 */
tasks.register("verifyOnDevice") {
    group = "verification"
    description = "Everything in `verify`, plus every module's instrumented tests. Needs a device."
    dependsOn("verify")
    dependsOn(
        subprojects
            .filter { it.buildFile.exists() && it.projectDir.resolve("src/androidTest").exists() }
            .map { "${it.path}:connectedDebugAndroidTest" },
    )
}
