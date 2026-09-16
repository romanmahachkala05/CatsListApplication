import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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

detekt {
    // Only the deviations are in the file; everything else keeps detekt's defaults.
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    // androidTest is not in the default source set, and the instrumented tests are the ones
    // guarding the data-loss paths. Note detekt's own defaults exclude test directories from
    // most naming and style rules — test code has different norms — so what this actually
    // brings to bear there are the correctness rules, which is the point.
    source.setFrom(
        "src/main/java",
        "src/test/java",
        "src/androidTest/java",
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        sarif.required.set(false)
        md.required.set(false)
    }
}

ktlint {
    // Pin the engine rather than inheriting whatever the plugin defaults to: a ktlint
    // upgrade that changes a rule should be a deliberate commit, not a surprise red build.
    version.set(libs.versions.ktlintEngine)
    // Fail the build. A formatting check that only warns is a formatting check nobody runs.
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
    filter {
        // Generated sources are not ours to format.
        exclude { it.file.path.contains("/build/") }
    }
}

room {
    // Committed history of every schema version, checked by Room at compile
    // time against each Migration and usable by MigrationTestHelper.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    // Coil
    implementation(platform(libs.coil.bom))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
