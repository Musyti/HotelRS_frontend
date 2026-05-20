package org.example.project

interface Platform {
    val name: String
    val baseUrl: String
}

expect fun getPlatform(): Platform