plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val firebaseEnabled = providers.gradleProperty("timegoFirebase").orNull == "true"
if (firebaseEnabled) {
    require(file("google-services.json").isFile) {
        "-PtimegoFirebase=true requires app/google-services.json; obtain it from the TimeGo Firebase project and do not commit it"
    }
    pluginManager.apply("com.google.gms.google-services")
}

val uploadStoreFilePath = providers.environmentVariable("TIMEGO_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.environmentVariable("TIMEGO_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("TIMEGO_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("TIMEGO_UPLOAD_KEY_PASSWORD").orNull
val hasCompleteUploadSigning = listOf(
    uploadStoreFilePath,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword,
).all { !it.isNullOrBlank() }

if (
    !hasCompleteUploadSigning &&
    gradle.startParameter.taskNames.any { requested -> requested.substringAfterLast(':') == "bundleRelease" }
) {
    throw GradleException(
        "bundleRelease requires all TIMEGO_UPLOAD_* environment variables; see docs/release/PLAY_RELEASE.md",
    )
}

android {
    namespace = "com.lsing.timego"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.lsing.timego"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.2"
        // targetSdk tracks compileSdk 37; keep them moving together (HeatP is also on 37).

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCompleteUploadSigning) {
            create("upload") {
                storeFile = file(requireNotNull(uploadStoreFilePath))
                storePassword = requireNotNull(uploadStorePassword)
                keyAlias = requireNotNull(uploadKeyAlias)
                keyPassword = requireNotNull(uploadKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            if (hasCompleteUploadSigning) {
                signingConfig = signingConfigs.getByName("upload")
            }
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
