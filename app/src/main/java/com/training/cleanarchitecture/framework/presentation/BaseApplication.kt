package com.training.cleanarchitecture.framework.presentation

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

open class BaseApplication : Application(){

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
    }

    private fun initCrashlytics() {
        FirebaseApp.initializeApp(applicationContext)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}