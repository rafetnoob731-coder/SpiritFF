package com.spirit.ff.service

import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Base64
import android.view.InputDevice
import android.view.MotionEvent
import com.spirit.ff.IUserService
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ShizukuUserService : IUserService.Stub() {

    override fun destroy() { System.exit(0) }

    override fun readMemory(pid: Int, address: Long, size: Int): String {
        return try {
            RandomAccessFile("/proc/$pid/mem", "r").use { mem ->
                mem.seek(address)
                val buf = ByteArray(size)
                mem.read(buf)
                Base64.encodeToString(buf, Base64.NO_WRAP)
            }
        } catch (e: Exception) { readViaDd(pid, address, size) }
    }

    private fun readViaDd(pid: Int, addr: Long, size: Int): String {
        return try {
            val p = Runtime.getRuntime().exec(
                arrayOf("dd","if=/proc/$pid/mem","bs=1","skip=$addr","count=$size","status=none"))
            val b = p.inputStream.readBytes(); p.waitFor()
            Base64.encodeToString(b, Base64.NO_WRAP)
        } catch (e: Exception) { "" }
    }

    private fun writeRaw(pid: Int, address: Long, data: ByteArray): Boolean {
        return try {
            RandomAccessFile("/proc/$pid/mem", "rw").use { mem ->
                mem.seek(address); mem.write(data) }
            true
        } catch (e: Exception) { writeViaDd(pid, address, data) }
    }

    private fun writeViaDd(pid: Int, addr: Long, data: ByteArray): Boolean {
        return try {
            val tmp = File.createTempFile("wm_", ".bin")
            tmp.writeBytes(data)
            val p = Runtime.getRuntime().exec(arrayOf("sh","-c",
                "dd if=${tmp.absolutePath} of=/proc/$pid/mem bs=1 seek=$addr conv=notrunc status=none"))
            val ok = p.waitFor() == 0; tmp.delete(); ok
        } catch (e: Exception) { false }
    }

    override fun writeFloat(pid: Int, address: Long, value: Float): Boolean {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); buf.putFloat(value)
        return writeRaw(pid, address, buf.array())
    }

    override fun writeInt(pid: Int, address: Long, value: Int): Boolean {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); buf.putInt(value)
        return writeRaw(pid, address, buf.array())
    }

    override fun getPidByPackage(packageName: String): Int {
        return try {
            Runtime.getRuntime().exec(arrayOf("pidof", packageName))
                .inputStream.bufferedReader().readLine()
                ?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull() ?: -1
        } catch (e: Exception) { -1 }
    }

    override fun getModuleBase(pid: Int, moduleName: String): Long {
        return try {
            File("/proc/$pid/maps").readLines()
                .firstOrNull { it.contains(moduleName) && it.contains("r-xp") }
                ?.split("-")?.firstOrNull()?.trim()
                ?.let { java.lang.Long.parseLong(it, 16) } ?: 0L
        } catch (e: Exception) { 0L }
    }

    override fun isProcessRunning(pid: Int): Boolean = File("/proc/$pid").exists()

    override fun injectTouch(x: Float, y: Float, ex: Float, ey: Float, ms: Int): Boolean {
        return try {
            val im = InputManager::class.java.getDeclaredMethod("getInstance").apply { isAccessible = true }.invoke(null)
            val inject = im.javaClass.getDeclaredMethod("injectInputEvent",
                android.view.InputEvent::class.java, Int::class.javaPrimitiveType
            ).apply { isAccessible = true }
            val down = SystemClock.uptimeMillis()
            fun send(action: Int, px: Float, py: Float, t: Long) {
                val ev = MotionEvent.obtain(down, t, action, px, py, 0)
                ev.source = InputDevice.SOURCE_TOUCHSCREEN
                inject.invoke(im, ev, 0); ev.recycle()
            }
            send(MotionEvent.ACTION_DOWN, x, y, down)
            val steps = maxOf(1, ms / 16)
            for (i in 1..steps) {
                val f = i.toFloat() / steps
                send(MotionEvent.ACTION_MOVE, x+(ex-x)*f, y+(ey-y)*f, down+i*16L)
                Thread.sleep(16)
            }
            send(MotionEvent.ACTION_UP, ex, ey, down+ms)
            true
        } catch (e: Exception) { false }
    }
}
