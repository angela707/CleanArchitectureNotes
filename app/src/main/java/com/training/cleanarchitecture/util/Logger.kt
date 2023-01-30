package com.training.cleanarchitecture.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.training.cleanarchitecture.util.Constants.DEBUG
import com.training.cleanarchitecture.util.Constants.TAG

var isUnitTest = false

fun printLogD(className: String?, message: String ) {
    if (DEBUG && !isUnitTest) {
        Log.d(TAG, "$className: $message")
    }
    else if(DEBUG && isUnitTest){
        println("$className: $message")
    }
}

fun cLog(msg: String?){
    msg?.let {
        if(!DEBUG){
            FirebaseCrashlytics.getInstance().log(it)
        }
    }

}
