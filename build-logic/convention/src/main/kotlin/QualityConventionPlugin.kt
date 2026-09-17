import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * ktlint + detekt, identically configured in every module. Applied by both library convention
 * plugins so no module opts out by omission.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jlleitschuh.gradle.ktlint")
                apply("io.gitlab.arturbosch.detekt")
            }

            extensions.configure<KtlintExtension> {
                // Pin the engine rather than inheriting whatever the plugin defaults to: a
                // ktlint upgrade that changes a rule should be a deliberate commit, not a
                // surprise red build.
                version.set(versionCatalog.findVersion("ktlintEngine").get().requiredVersion)
                // Fail the build. A formatting check that only warns is a formatting check
                // nobody runs.
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
                // Only the deviations are in the file; everything else keeps detekt's defaults.
                buildUponDefaultConfig = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                // androidTest is not in the default source set, and in :core:data that is
                // where the migration/upgrade tests guarding the data-loss paths live.
                source.setFrom(
                    listOf("src/main", "src/test", "src/androidTest")
                        .map { project.file(it) }
                        .filter { it.exists() },
                )
            }

            tasks.withType<Detekt>().configureEach {
                jvmTarget = "17"
                reports {
                    html.required.set(true)
                    sarif.required.set(false)
                    md.required.set(false)
                }
            }
        }
    }
}
