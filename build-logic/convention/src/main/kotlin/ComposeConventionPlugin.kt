import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Additive: applied alongside `catslist.android.library` by modules that render Compose UI. */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            val bom = versionCatalog.findLibrary("androidx-compose-bom").get()
            dependencies.add("implementation", dependencies.platform(bom))
        }
    }
}
