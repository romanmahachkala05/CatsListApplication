plugins {
    id("catslist.android.library")
    // catslist.hilt already applies the KSP plugin; Room's annotation processing reuses it.
    id("catslist.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.example.catslist.core.data"
}

room {
    // Committed history of every schema version, checked by Room at compile
    // time against each Migration and usable by MigrationTestHelper.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
