import com.android.build.api.dsl.LibraryExtension
import com.example.huellitasya.buildlogic.configureAndroidCommon
import com.example.huellitasya.buildlogic.lib
import com.example.huellitasya.buildlogic.libs
import com.example.huellitasya.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))

        extensions.configure<LibraryExtension> {
            configureAndroidCommon()
        }

        dependencies {
            add("testImplementation", libs.lib("junit"))
        }
    }
}
