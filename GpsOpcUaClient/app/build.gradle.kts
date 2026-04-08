plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.projeto.gpsopcua"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.projeto.gpsopcua"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
    packaging {
        resources {
            pickFirsts.add("META-INF/io.netty.versions.properties")
            pickFirsts.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/NOTICE.md")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.eclipse.milo:sdk-client:0.6.12")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.multidex:multidex:2.0.1")
}