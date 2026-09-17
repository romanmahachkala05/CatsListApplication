plugins {
    id("catslist.android.library")
    id("catslist.compose")
    id("catslist.hilt")
    // CatsListNavKey is @Serializable, for Navigation 3's saved-state support.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.catslist.feature.feed"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.collections.immutable)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
