import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Used by every module except `:app` (which stays an application module for signing/versioning)
 * and `:core:model` (which stays framework-free) — the shared Android defaults only.
 * Deliberately no Compose or Hilt here: `:core:data` has no UI and nothing to inject, so both
 * are separate, additive plugins ([ComposeConventionPlugin], [HiltConventionPlugin]) applied
 * only by the modules that actually use them.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("catslist.quality")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = 36

                defaultConfig {
                    minSdk = 27
                    // Without this, AGP falls back to the legacy `android.test.InstrumentationTestRunner`,
                    // which does not discover JUnit4 `@RunWith(AndroidJUnit4::class)` tests — instrumented
                    // tests then silently run zero tests and report success. Bit twice: once here, once
                    // when this module's androidTest sources were still part of `:app` and it had this
                    // set directly.
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                testOptions {
                    unitTests {
                        // Code that logs failures through android.util.Log — a stub on the JVM
                        // that throws by default — stays testable without Robolectric.
                        isReturnDefaultValues = true
                    }
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
}
