package com.example.assignmate

import android.app.Application
import com.cloudinary.android.MediaManager

class AssignMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config: MutableMap<String, Any> = HashMap()
        config["cloud_name"] = "dtdhkcyg1"
        MediaManager.init(this, config)
    }
}
