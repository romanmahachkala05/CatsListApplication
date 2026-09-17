plugins {
    id("catslist.android.library")
}

android {
    namespace = "com.example.catslist.core.testing"
}

dependencies {
    // Fakes implement the contracts :core:data/:core:ui own (CatRepository, CatApiService,
    // CatDao, ImageDownloader, SnackbarNotifier), so this depends on them rather than the
    // reverse. :core:ui's own tests depend back on this module — that's a test-to-main
    // dependency in the other direction, not a cycle.
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:ui"))

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.collections.immutable)
}
