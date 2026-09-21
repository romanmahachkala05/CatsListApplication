import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/** ktlint + detekt, identically configured in every module by the library convention plugins. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jlleitschuh.gradle.ktlint")
                apply("dev.detekt")
            }

            extensions.configure<KtlintExtension> {
                // Pinned, so a rule change arrives as a deliberate commit and not a red build.
                version.set(versionCatalog.findVersion("ktlintEngine").get().requiredVersion)
                // A formatting check that only warns is one nobody runs.
                ignoreFailures.set(false)
                reporters {
                    reporter(ReporterType.PLAIN)
                }
                filter {
                    // Generated sources are not ours to format.
                    exclude { it.file.path.contains("/build/") }
                }
            }

            extensions.configure<DetektExtension> {
                // Only the deviations are in the file; the rest are detekt's defaults.
                buildUponDefaultConfig.set(true)
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                // androidTest is not in the default source set, and :core:data keeps its
                // migration tests there.
                source.setFrom(
                    listOf("src/main", "src/test", "src/androidTest")
                        .map { project.file(it) }
                        .filter { it.exists() },
                )
            }

            tasks.withType<Detekt>().configureEach {
                jvmTarget.set("17")
                reports {
                    html.required.set(true)
                    sarif.required.set(false)
                    markdown.required.set(false)
                }
            }
        }
    }
}
