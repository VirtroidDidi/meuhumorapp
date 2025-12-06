plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.apphumor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.apphumor"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // WorkManager (Para agendamento de tarefas em background)
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Coroutines para Firebase (tasks.await)
    implementation(libs.kotlinx.coroutines.play.services)

    // Ciclo de vida e ViewModel Scope (versão mais recente que a sua atual)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Se o seu targetSdk for 33 ou mais, você pode precisar disto:
    implementation(libs.androidx.activity.ktx.v181)




    // Firebase BoM (Bill of Materials)
    implementation(platform(libs.firebase.bom.v3273)) // Atualizado para a versão 32.7.3

    // Firebase Analytics (opcional, mas recomendado)
    implementation(libs.google.firebase.analytics.ktx)

    // Firebase Authentication
    implementation(libs.google.firebase.auth.ktx)

    // Firebase Firestore (Sem exclusão de firebase-common)
    implementation(libs.com.google.firebase.firebase.firestore.ktx)

    // Firebase Realtime Database
    implementation(libs.google.firebase.database.ktx)

    // Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // Dependências do AndroidX (Removidas as versões fixas)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // Material Design
    implementation(libs.material.v1120)


}