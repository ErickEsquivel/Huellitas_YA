plugins {
    alias(libs.plugins.huellitas.android.feature)
}

android {
    namespace = "com.example.huellitasya.feature.auth"
}

dependencies {
    // Selector de cuentas de Google (Credential Manager). Es una API de UI: necesita un Activity.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
