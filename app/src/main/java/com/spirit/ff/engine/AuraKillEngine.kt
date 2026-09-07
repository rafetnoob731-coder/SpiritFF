package com.spirit.ff.engine

import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.MemoryWriter
import com.spirit.ff.memory.Offsets
import kotlin.math.sqrt

class AuraKillEngine(private val mem: MemoryReader, private val wrt: MemoryWriter) {
    @Volatile var running = false
    private var thread: Thread? = null
    fun start(){if(running)return;running=true;thread=Thread(::loop,"aura").apply{isDaemon=true;start()}}
    fun stop(){running=false;thread?.interrupt();thread=null}
    private fun loop(){while(running){try{tick();Thread.sleep(BypassConfig.auraTickMs)}catch(_:InterruptedException){break}}}
    private fun tick(){
        val base=mem.base; val lp=mem.ptr(base+Offsets.LOCAL_PLAYER); if(lp==0L)return
        if(mem.ptr(lp+Offsets.ENT_WEAPON_SLOT)==0L)return
        val lx=mem.float(lp+Offsets.ENT_POS_X); val ly=mem.float(lp+Offsets.ENT_POS_Y); val lz=mem.float(lp+Offsets.ENT_POS_Z)
        val lt=mem.int(lp+Offsets.ENT_TEAM); val list=mem.ptr(base+Offsets.ENTITY_LIST); val cnt=mem.int(base+Offsets.ENTITY_COUNT).coerceIn(0,60)
        for(i in 0 until cnt){
            val ep=mem.ptr(list+i*8); if(ep==0L||ep==lp)continue
            val hp=mem.int(ep+Offsets.ENT_HEALTH); val team=mem.int(ep+Offsets.ENT_TEAM); if(hp<=0||team==lt)continue
            val ex=mem.float(ep+Offsets.ENT_POS_X); val ey=mem.float(ep+Offsets.ENT_POS_Y); val ez=mem.float(ep+Offsets.ENT_POS_Z)
            if(sqrt((ex-lx)*(ex-lx)+(ey-ly)*(ey-ly)+(ez-lz)*(ez-lz))<=BypassConfig.auraRadius) wrt.int(ep+Offsets.ENT_HEALTH,0)
        }
    }
}
