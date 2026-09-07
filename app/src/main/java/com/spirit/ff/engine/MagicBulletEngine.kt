package com.spirit.ff.engine

import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.MemoryWriter
import com.spirit.ff.memory.Offsets

class MagicBulletEngine(private val mem: MemoryReader, private val wrt: MemoryWriter) {
    @Volatile var running = false
    private var thread: Thread? = null
    private val saved = mutableMapOf<Long,Float>()
    fun start(){if(running)return;running=true;thread=Thread(::loop,"magic").apply{isDaemon=true;start()}}
    fun stop(){running=false;thread?.interrupt();thread=null;saved.forEach{(a,v)->wrt.float(a,v)};saved.clear()}
    private fun loop(){while(running){try{tick();Thread.sleep(BypassConfig.magicTickMs)}catch(_:InterruptedException){break}}}
    private fun tick(){
        val base=mem.base; val lp=mem.ptr(base+Offsets.LOCAL_PLAYER); if(lp==0L)return
        val wp=mem.ptr(lp+Offsets.ENT_WEAPON_SLOT); if(wp==0L)return
        wrt.float(wp+Offsets.WPN_SPREAD,0f)
        wrt.float(wp+Offsets.WPN_BULLET_SPEED,BypassConfig.bulletSpeed)
        wrt.float(wp+Offsets.WPN_RANGE,99999f)
        val da=wp+Offsets.WPN_DAMAGE
        val orig=saved.getOrPut(da){val v=mem.float(da); if(v>0f)v else 1f}
        val mod=orig*BypassConfig.damageMultiplier
        Thread{wrt.float(da,mod);Thread.sleep(8);wrt.float(da,orig)}.apply{isDaemon=true}.start()
    }
}
