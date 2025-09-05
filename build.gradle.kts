buildscript {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://plugins.gradle.org/m2/")
        // repo which contains Cloudstream's tools and dependencies
        maven("https://jitpack.io")
    }
    dependencies {
        classpath(libs.gradle.agp)
        classpath(libs.gradle.kotlin)
        classpath(libs.gradle.kotlin.serialization)
        classpath(libs.gradle.kotlinter)
    }
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }
}
