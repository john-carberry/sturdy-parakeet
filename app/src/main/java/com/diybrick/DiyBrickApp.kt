package com.diybrick

import android.app.Application

class DiyBrickApp : Application() {
    val container by lazy { AppContainer(this) }
}
