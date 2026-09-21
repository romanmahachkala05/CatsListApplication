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

    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // `api`, not `implementation`: PagingData is part of CatRepository's return type, so
    // every consumer needs it on the classpath.
    api(libs.androidx.paging.runtime)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.paging.testing)

    // Same shared fakes the unit tests use (ADR-0012); the instrumented ones need
    // FakeNetworkMonitor to build an ErrorMapper.
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.paging.testing)
}
