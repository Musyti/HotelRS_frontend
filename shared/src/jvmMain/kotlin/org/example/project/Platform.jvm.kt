package org.example.project

class JVMPlatform(override val baseUrl: String) : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform("http://127.0.0.1:8080")