import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Not just for CatDto (which moved to :core:data with it) — CatsListNavKey and
    // FavoriteCatsNavKey are @Serializable too, for Navigation 3's saved-state support, and
    // stayed here. Removing this when CatDto left broke navigation at runtime, not compile
    // time: @Serializable without the compiler plugin fails only when something actually
    // looks the serializer up.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("catslist.quality")
}

// Declared once, in parts, with versionCode derived from them. The 2022 app shipped
// versionCode 1 for *both* v1.0.1 and v1.0.2: Play rejects an upload whose versionCode has
// not increased, so the second of those could never have shipped. Deriving the number means
// the two cannot drift apart again — bumping the name necessarily bumps the code.
//
// Minor and patch are allowed 0-99 each, which is a wider range than this project will use.
// Release signing credentials, if this machine has them. Read once, defensively: a fresh
// clone and CI have no keystore, and reading them unconditionally would fail *configuration*
// for everyone — not just release builds. `keystore.properties` is gitignored; CI would use
// the environment instead.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun releaseSigningValue(key: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv("CATSLIST_${key.uppercase()}")

/** Only true when every part is present; a half-configured signing config fails at package time. */
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !releaseSigningValue(it).isNullOrBlank() }

val versionMajor = 2
val versionMinor = 0
val versionPatch = 0

android {
    namespace = "com.example.catslist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.catslist"
        minSdk = 27
        targetSdk = 36
        versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseSigningValue("storeFile")!!)
                storePassword = releaseSigningValue("storePassword")
                keyAlias = releaseSigningValue("keyAlias")
                keyPassword = releaseSigningValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Null where no keystore is configured, which produces an unsigned release APK
            // rather than failing the build. That keeps `assembleRelease` runnable by anyone
            // who clones this, and means only a machine holding the key can ship a signed one.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            // ViewModels log failures through android.util.Log, which is a stub on the JVM and
            // throws by default. Returning defaults keeps those paths testable without Robolectric.
            isReturnDefaultValues = true
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:favorites"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // Hilt — MainActivity/App are @AndroidEntryPoint/@HiltAndroidApp; the ViewModels
    // themselves, and hiltViewModel(), now live in the feature modules.
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
