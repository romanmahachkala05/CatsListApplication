import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Additive: applied alongside `catslist.android.library` by modules that actually declare `@Module`s. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            dependencies {
                add("implementation", versionCatalog.findLibrary("hilt-android").get())
                add("ksp", versionCatalog.findLibrary("hilt-android-compiler").get())
            }
        }
    }
}
