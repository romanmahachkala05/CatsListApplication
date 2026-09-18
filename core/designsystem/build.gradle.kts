plugins {
    id("catslist.android.library")
    id("catslist.compose")
}

android {
    namespace = "com.example.catslist.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    // Coil auto-registers its network fetcher via ServiceLoader from whatever module declares
    // it — AsyncImage lives here, so the fetcher belongs here too, not in :app.
    implementation(libs.coil.network.okhttp)
}
