package dependencies

object Build {
    const val androidBuildTools = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val googleServices = "com.google.gms:google-services:${Versions.play_services}"
    const val junit5 = "de.mannodermaus.gradle.plugins:android-junit5:1.3.2.0"
    const val crashlytics_gradle = "com.google.firebase:firebase-crashlytics-gradle:${Versions.crashlytics_gradle}"
}