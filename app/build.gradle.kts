plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.timevault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.timevault"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation (libs.material.v1110)

    implementation(libs.lottie)

    implementation(platform(libs.firebase.bom)) // Check for the latest!


    implementation (libs.curvedbottomnavigation) //For Bottom Navigation
// (https://github.com/qamarelsafadi/CurvedBottomNavigation) Link of Github of that


    //Dependency of cloudinary
    implementation (libs.androidx.work.runtime.ktx)
    implementation (libs.okhttp) // For HTTP upload
    implementation (libs.cloudinary.android)


    //Dependency of Data and Time picker -> IOS-style wheel picker (Not Using ,problem in year displaying)
//    implementation (libs.singledateandtimepicker)

    //Dependency of Date and Time Picker -> Material Date/Time Picker (Android's Material Design)
    implementation (libs.material.v190)

    //Library for permission : Dexter
    implementation (libs.dexter)


}