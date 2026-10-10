// Utilidades compartidas de tests: fakes de repositorios y regla de dispatcher Main.
plugins {
    alias(libs.plugins.huellitas.jvm.library)
}

dependencies {
    api(project(":core:domain"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
