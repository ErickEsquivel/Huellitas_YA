import com.example.huellitasya.buildlogic.lib
import com.example.huellitasya.buildlogic.libs
import com.example.huellitasya.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Módulo de feature: librería Android + Compose + Hilt + navegación type-safe.
 * Según la spec 000, una feature solo depende de módulos core (nunca de :core:data ni de otra feature).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("huellitas.android.library")
        pluginManager.apply("huellitas.android.compose")
        pluginManager.apply("huellitas.hilt")
        pluginManager.apply(libs.pluginId("kotlin-serialization"))

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))

            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("kotlinx-serialization-json"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
