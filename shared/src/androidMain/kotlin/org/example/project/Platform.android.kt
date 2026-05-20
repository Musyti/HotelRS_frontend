package org.example.project

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val baseUrl: String = "http://192.168.1.113:8080"  // Твой IP
}

actual fun getPlatform(): Platform = AndroidPlatform()