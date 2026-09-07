package com.spirit.ff.input

import com.spirit.ff.IUserService

class InputInjector(private val svc: IUserService) {
    fun swipe(x: Float, y: Float, ex: Float, ey: Float, ms: Int = 60) {
        svc.injectTouch(x, y, ex, ey, ms)
    }
}
