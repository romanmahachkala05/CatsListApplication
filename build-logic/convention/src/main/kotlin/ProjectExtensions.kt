import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * The root `libs.versions.toml` catalog, for convention plugin code to read.
 *
 * Deliberately not named `libs`: every module applying one of these plugins has that plugin's
 * classes on its build script's classpath, so a same-named top-level extension here would
 * shadow Gradle's own generated type-safe `libs` accessor in that module's build.gradle.kts —
 * silently, since Kotlin resolves it rather than reporting the clash as ambiguous.
 */
val Project.versionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
