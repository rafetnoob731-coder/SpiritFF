package com.spirit.ff.engine

import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.MemoryWriter
import com.spirit.ff.memory.Offsets

class FastSniperEngine(private val mem: MemoryReader, private val wrt: MemoryWriter) {
    @Volatile var running = false
    private var thread: Thread? = null
    fun start(){if(running)return;running=true;thread=Thread(::loop,"sniper").apply{isDaemon=true;start()}}
    fun stop(){running=false;thread?.interrupt();thread=null}
    private fun loop(){while(running){try{tick();Thread.sleep(BypassConfig.sniperTickMs)}catch(_:InterruptedException){break}}}
    private fun tick(){
        val base=mem.base; val lp=mem.ptr(base+Offsets.LOCAL_PLAYER); if(lp==0L)return
        val wp=mem.ptr(lp+Offsets.ENT_WEAPON_SLOT); if(wp==0L)return
        wrt.float(wp+Offsets.WPN_SWITCH_TIME,BypassConfig.switchTime)
        if(mem.int(wp+Offsets.WPN_TYPE)==Offsets.WPN_TYPE_SNIPER){
            val orig=mem.float(wp+Offsets.WPN_RELOAD_TIME)
            wrt.float(wp+Offsets.WPN_RELOAD_TIME,if(orig>0.5f)orig*BypassConfig.reloadTimeMult else BypassConfig.reloadTimeMult)
            val max=mem.int(wp+Offsets.WPN_MAX_AMMO); if(max>0)wrt.int(wp+Offsets.WPN_AMMO,max)
        }
    }
}
