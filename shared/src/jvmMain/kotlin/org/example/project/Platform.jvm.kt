package org.example.project

class JVMPlatform(override val baseUrl: String) : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform("http://185.105.89.51:8080")