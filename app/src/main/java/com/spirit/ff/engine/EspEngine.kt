package com.spirit.ff.engine

import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.Offsets

data class RadarDot(val relX: Float, val relZ: Float, val visible: Boolean)

object EspData {
    val dots = mutableListOf<RadarDot>()
    var lock = Any()
    @Volatile var debug: String = "idle"
    @Volatile var lastCount: Int = -1
}

class EspEngine(private val mem: MemoryReader) {
    @Volatile var running = false
    private var thread: Thread? = null
    fun start() { if (running) return; running = true; thread = Thread(::loop,"esp").apply{isDaemon=true;start()} }
    fun stop()  { running = false; thread?.interrupt(); thread = null; synchronized(EspData.lock){EspData.dots.clear()} }
    private fun loop() { while(running){try{tick();Thread.sleep(BypassConfig.espTickMs)}catch(_:InterruptedException){break}} }
    private fun tick() {
        val base=mem.base; val localPtr=mem.ptr(base+Offsets.LOCAL_PLAYER)
        if(localPtr==0L){EspData.debug="local=0 (LOCAL_PLAYER off?)";return}
        val lx=mem.float(localPtr+Offsets.ENT_POS_X); val lz=mem.float(localPtr+Offsets.ENT_POS_Z)
        val localTeam=mem.int(localPtr+Offsets.ENT_TEAM)
        val listPtr=mem.ptr(base+Offsets.ENTITY_LIST); val count=mem.int(base+Offsets.ENTITY_COUNT).coerceIn(0,60)
        EspData.lastCount=count
        if(listPtr==0L){EspData.debug="list=0 cnt=$count";return}
        if(count==0){EspData.debug="cnt=0 in lobby?";return}
        val dots=mutableListOf<RadarDot>(); val range=400f
        for(i in 0 until count){
            val ep=mem.ptr(listPtr+i*8); if(ep==0L||ep==localPtr)continue
            val hp=mem.int(ep+Offsets.ENT_HEALTH); val team=mem.int(ep+Offsets.ENT_TEAM)
            if(hp<=0||team==localTeam)continue
            val ex=mem.float(ep+Offsets.ENT_POS_X); val ez=mem.float(ep+Offsets.ENT_POS_Z)
            val vis=mem.int(ep+Offsets.ENT_IS_VISIBLE)==1
            dots+=RadarDot(((ex-lx)/range).coerceIn(-1f,1f),((ez-lz)/range).coerceIn(-1f,1f),vis)
        }
        synchronized(EspData.lock){EspData.dots.clear();EspData.dots.addAll(dots)}
        EspData.debug="ok cnt=$count shown=${dots.size} lx=$lx lz=$lz"
    }
}
