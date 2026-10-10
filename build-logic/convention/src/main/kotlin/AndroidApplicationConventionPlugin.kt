import com.android.build.api.dsl.ApplicationExtension
import com.example.huellitasya.buildlogic.HuellitasConfig
import com.example.huellitasya.buildlogic.configureAndroidCommon
import com.example.huellitasya.buildlogic.libs
import com.example.huellitasya.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-application"))

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon()
            defaultConfig.targetSdk = HuellitasConfig.TARGET_SDK
        }
    }
}
