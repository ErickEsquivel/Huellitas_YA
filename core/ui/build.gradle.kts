plugins {
    alias(libs.plugins.huellitas.android.library)
    alias(libs.plugins.huellitas.android.compose)
}

android {
    namespace = "com.example.huellitasya.core.ui"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:designsystem"))
    api(libs.androidx.lifecycle.runtime.compose)
}
