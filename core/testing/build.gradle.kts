plugins {
    id("catslist.android.library")
}

android {
    namespace = "com.example.catslist.core.testing"
}

dependencies {
    // Fakes implement the contracts :core:data owns (CatRepository, CatApiService, CatDao,
    // ImageDownloader), so this depends on it rather than the reverse.
    api(project(":core:model"))
    api(project(":core:data"))

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.collections.immutable)
}
