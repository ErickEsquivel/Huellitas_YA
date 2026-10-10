plugins {
    alias(libs.plugins.huellitas.jvm.library)
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(project(":core:testing"))
}
