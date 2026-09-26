package com.livefree

import android.app.Application

class LiveFreeApp : Application() {
    val container by lazy { AppContainer(this) }
}
