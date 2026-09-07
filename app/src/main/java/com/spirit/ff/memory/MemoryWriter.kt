package com.spirit.ff.memory

import com.spirit.ff.IUserService

class MemoryWriter(private val svc: IUserService, val pid: Int) {
    fun float(addr: Long, v: Float)  = svc.writeFloat(pid, addr, v)
    fun int(addr: Long, v: Int)      = svc.writeInt(pid, addr, v)
}
