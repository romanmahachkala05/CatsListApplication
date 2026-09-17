import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** The root `libs.versions.toml` catalog, made available to convention plugin code. */
val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
