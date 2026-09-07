package com.spirit.ff.engine

import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.MemoryWriter
import com.spirit.ff.memory.Offsets
import kotlin.math.*

class TeleportEngine(private val mem: MemoryReader, private val wrt: MemoryWriter) {
    @Volatile var teleporting = false
    fun teleportToNearestLoot() {
        if (teleporting) return
        val base=mem.base; val localPtr=mem.ptr(base+Offsets.LOCAL_PLAYER); if(localPtr==0L)return
        val lx=mem.float(localPtr+Offsets.ENT_POS_X); val ly=mem.float(localPtr+Offsets.ENT_POS_Y); val lz=mem.float(localPtr+Offsets.ENT_POS_Z)
        val target=findLoot(base,lx,ly,lz)?:return
        val dist=sqrt((target.first-lx).pow(2)+(target.second-ly).pow(2)+(target.third-lz).pow(2))
        if(dist<BypassConfig.minTpDistance)return
        val steps=ceil(dist/BypassConfig.maxTpStep).toInt().coerceIn(1,80)
        Thread{teleporting=true;try{for(i in 1..steps){val t=i.toFloat()/steps;wrt.float(localPtr+Offsets.ENT_POS_X,lx+(target.first-lx)*t);wrt.float(localPtr+Offsets.ENT_POS_Y,ly+(target.second-ly)*t+1.2f);wrt.float(localPtr+Offsets.ENT_POS_Z,lz+(target.third-lz)*t);Thread.sleep(BypassConfig.stepDelayMs)}}finally{teleporting=false}}.apply{isDaemon=true}.start()
    }
    private fun findLoot(base:Long,lx:Float,ly:Float,lz:Float):Triple<Float,Float,Float>?{
        val ll=mem.ptr(base+Offsets.LOOT_LIST); val lc=mem.int(base+Offsets.LOOT_COUNT).coerceIn(0,500); if(ll==0L||lc==0)return null
        var bd=Float.MAX_VALUE; var best:Triple<Float,Float,Float>?=null
        for(i in 0 until lc){val p=mem.ptr(ll+i*8);if(p==0L)continue;val ix=mem.float(p+Offsets.LOOT_POS_X);val iy=mem.float(p+Offsets.LOOT_POS_Y);val iz=mem.float(p+Offsets.LOOT_POS_Z);val d=sqrt((ix-lx).pow(2)+(iy-ly).pow(2)+(iz-lz).pow(2));if(d<bd){bd=d;best=Triple(ix,iy,iz)}}
        return best
    }
}
