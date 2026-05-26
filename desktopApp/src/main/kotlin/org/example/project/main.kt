package org.example.project

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        icon = painterResource("logo.ico"),
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {
        App()
    }
}