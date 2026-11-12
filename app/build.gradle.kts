import java.util.Properties
plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
}
val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
fun config(key: String) = "\"" + local.getProperty(key, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\""
android {
    namespace = "br.com.carrinhocerto"
    compileSdk = 35
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "br.com.carrinhocerto"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        buildConfigField("String", "SUPABASE_URL", config("supabase.url"))
        buildConfigField("String", "SUPABASE_KEY", config("supabase.anonKey"))
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].java.srcDir("src/main/kotlin")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.material3); implementation(libs.ui); implementation(libs.tooling)
    implementation(libs.activity); implementation(libs.lifecycle); implementation(libs.viewmodel)
    implementation(libs.camera.core); implementation(libs.camera.camera2); implementation(libs.camera.lifecycle); implementation(libs.camera.view)
    implementation(libs.barcode); implementation(libs.ocr)
    implementation(libs.room.runtime); implementation(libs.room.ktx); ksp(libs.room.compiler)
    implementation(libs.koin); implementation(libs.retrofit); implementation(libs.converter); implementation(libs.json)
    implementation(libs.coil); implementation(libs.work)
    testImplementation(libs.junit)
}
