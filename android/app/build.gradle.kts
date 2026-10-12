plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace="id.edukreativa.korektorku"
    compileSdk=35
    defaultConfig { applicationId="id.edukreativa.korektorku"; minSdk=28; targetSdk=35; versionCode=5; versionName="0.5.0-mvp" }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget="17" }
    buildTypes { release { isMinifyEnabled=false } }
}

dependencies { testImplementation("junit:junit:4.13.2") }
