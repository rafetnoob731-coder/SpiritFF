package com.spirit.ff.memory

import android.util.Base64
import com.spirit.ff.IUserService
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MemoryReader(val svc: IUserService) {

    var pid: Int   = -1
    var base: Long = 0L

    fun init(): Boolean {
        pid = svc.getPidByPackage("com.dts.freefireth")
            .takeIf { it > 0 }
            ?: svc.getPidByPackage("com.dts.freefiremax").takeIf { it > 0 }
            ?: -1
        if (pid <= 0) return false
        base = svc.getModuleBase(pid, "libil2cpp.so")
        return base != 0L
    }

    fun alive(): Boolean = pid > 0 && svc.isProcessRunning(pid)

    private fun raw(addr: Long, size: Int): ByteArray {
        val b64 = svc.readMemory(pid, addr, size)
        return if (b64.isNullOrEmpty()) ByteArray(size)
        else Base64.decode(b64, Base64.NO_WRAP)
    }

    private fun buf(addr: Long, size: Int) =
        ByteBuffer.wrap(raw(addr, size)).order(ByteOrder.LITTLE_ENDIAN)

    fun float(addr: Long)  = runCatching { buf(addr, 4).float  }.getOrDefault(0f)
    fun int(addr: Long)    = runCatching { buf(addr, 4).int    }.getOrDefault(0)
    fun long64(addr: Long) = runCatching { buf(addr, 8).long   }.getOrDefault(0L)
    fun ptr(addr: Long)    = long64(addr)
}
