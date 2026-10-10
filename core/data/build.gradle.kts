plugins {
    alias(libs.plugins.huellitas.android.library)
    alias(libs.plugins.huellitas.hilt)
}

android {
    namespace = "com.example.huellitasya.core.data"
}

dependencies {
    api(project(":core:domain"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
}
