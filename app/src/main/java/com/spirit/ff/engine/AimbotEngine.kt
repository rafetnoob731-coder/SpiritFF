package com.spirit.ff.engine

import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.MemoryWriter
import com.spirit.ff.memory.Offsets
import kotlin.math.*

class AimbotEngine(private val mem: MemoryReader, private val wrt: MemoryWriter) {
    @Volatile var running = false
    private var thread: Thread? = null
    private var currentPitch = 0f; private var currentYaw = 0f
    private var lastTarget = 0L; private var targetAcquiredAt = 0L; private var shotCount = 0

    fun start() { if (running) return; running = true; thread = Thread(::loop, "aimbot").apply { isDaemon = true; start() } }
    fun stop()  { running = false; thread?.interrupt(); thread = null; currentPitch = 0f; currentYaw = 0f }

    private fun loop() { while (running) { try { tick(); Thread.sleep(BypassConfig.aimbotTickMs) } catch (_: InterruptedException) { break } } }

    private fun tick() {
        val base = mem.base
        val localPtr = mem.ptr(base + Offsets.LOCAL_PLAYER); if (localPtr == 0L) return
        val localHead = headPos(localPtr) ?: return
        val localTeam = mem.int(localPtr + Offsets.ENT_TEAM)
        val camBase = mem.ptr(base + Offsets.CAMERA_MGR); if (camBase == 0L) return
        val currentCamYaw = mem.float(camBase + Offsets.CAM_YAW)
        val listPtr = mem.ptr(base + Offsets.ENTITY_LIST)
        val count   = mem.int(base + Offsets.ENTITY_COUNT).coerceIn(0, 60)
        var bestPtr = 0L; var bestDist = Float.MAX_VALUE
        for (i in 0 until count) {
            val entPtr = mem.ptr(listPtr + i * 8); if (entPtr == 0L || entPtr == localPtr) continue
            val hp = mem.int(entPtr + Offsets.ENT_HEALTH); val team = mem.int(entPtr + Offsets.ENT_TEAM)
            if (hp <= 0 || team == localTeam) continue
            val head = headPos(entPtr) ?: continue
            val ta = calcAngles(localHead, head); if (!isInFov(currentCamYaw, ta.second)) continue
            val d = dist3(localHead, head); if (d < bestDist) { bestDist = d; bestPtr = entPtr }
        }
        if (bestPtr == 0L) { lastTarget = 0L; return }
        if (bestPtr != lastTarget) { targetAcquiredAt = System.currentTimeMillis(); lastTarget = bestPtr; return }
        if (System.currentTimeMillis() - targetAcquiredAt < BypassConfig.aimbotReactionMs) return
        val targetHead = headPos(bestPtr) ?: return
        shotCount++
        val useChest = (shotCount % BypassConfig.missEvery == 0)
        val aimPos = if (useChest) {
            val bp = mem.ptr(bestPtr + Offsets.ENT_BONE_MATRIX)
            if (bp != 0L) { val o = 6 * Offsets.BONE_STRIDE; Triple(mem.float(bp+o+12), mem.float(bp+o+28), mem.float(bp+o+44)) } else targetHead
        } else targetHead
        val angles = addNoise(calcAngles(localHead, aimPos))
        currentPitch += (angles.first  - currentPitch) * BypassConfig.aimbotSmooth
        currentYaw   += (angles.second - currentYaw)   * BypassConfig.aimbotSmooth
        wrt.float(camBase + Offsets.CAM_PITCH, currentPitch)
        wrt.float(camBase + Offsets.CAM_YAW,   currentYaw)
    }

    private fun headPos(p: Long): Triple<Float,Float,Float>? {
        val bp = mem.ptr(p + Offsets.ENT_BONE_MATRIX); if (bp == 0L) return null
        val o = Offsets.HEAD_BONE_IDX * Offsets.BONE_STRIDE
        return Triple(mem.float(bp+o+12), mem.float(bp+o+28), mem.float(bp+o+44))
    }
    private fun calcAngles(f: Triple<Float,Float,Float>, t: Triple<Float,Float,Float>): Pair<Float,Float> {
        val dx=t.first-f.first; val dy=t.second-f.second; val dz=t.third-f.third; val dxz=sqrt(dx*dx+dz*dz)
        return Pair((-Math.toDegrees(atan2(dy.toDouble(),dxz.toDouble()))).toFloat(), Math.toDegrees(atan2(dx.toDouble(),dz.toDouble())).toFloat())
    }
    private fun isInFov(cy: Float, ty: Float): Boolean { var d=abs(ty-cy)%360f; if(d>180f)d=360f-d; return d<=BypassConfig.aimbotFov }
    private fun addNoise(a: Pair<Float,Float>): Pair<Float,Float> { val r=java.util.Random(); val n=0.25f; return Pair(a.first+(r.nextFloat()-0.5f)*n, a.second+(r.nextFloat()-0.5f)*n) }
    private fun dist3(a: Triple<Float,Float,Float>, b: Triple<Float,Float,Float>): Float { val dx=b.first-a.first; val dy=b.second-a.second; val dz=b.third-a.third; return sqrt(dx*dx+dy*dy+dz*dz) }
}
