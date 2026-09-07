package com.spirit.ff.memory

object Offsets {
    // Entity System
    const val ENTITY_LIST        = 0x05A3F2C0L
    const val ENTITY_COUNT       = 0x05A3F2C8L
    const val LOCAL_PLAYER       = 0x05A3E8B0L

    // Entity Fields
    const val ENT_HEALTH         = 0x110L
    const val ENT_MAX_HEALTH     = 0x114L
    const val ENT_TEAM           = 0x12CL
    const val ENT_POS_X          = 0x130L
    const val ENT_POS_Y          = 0x134L
    const val ENT_POS_Z          = 0x138L
    const val ENT_IS_ALIVE       = 0x108L
    const val ENT_IS_VISIBLE     = 0x3C0L
    const val ENT_BONE_MATRIX    = 0x1A8L
    const val ENT_WEAPON_SLOT    = 0x3A0L

    // Head bone index 7 in FF rig; each bone = 64 bytes
    const val HEAD_BONE_IDX      = 7
    const val BONE_STRIDE        = 64L

    // Camera
    const val CAMERA_MGR         = 0x05B1C3D0L
    const val CAM_PITCH          = 0x2ACL
    const val CAM_YAW            = 0x2B0L
    const val CAM_FOV            = 0x2B4L

    // Weapon
    const val WPN_TYPE           = 0x190L
    const val WPN_DAMAGE         = 0x1A0L
    const val WPN_RANGE          = 0x1A4L
    const val WPN_BULLET_SPEED   = 0x1ACL
    const val WPN_SPREAD         = 0x228L
    const val WPN_RELOAD_TIME    = 0x1BCL
    const val WPN_SWITCH_TIME    = 0x1C4L
    const val WPN_AMMO           = 0x1D0L
    const val WPN_MAX_AMMO       = 0x1D4L
    const val WPN_TYPE_SNIPER    = 3

    // Loot
    const val LOOT_LIST          = 0x05C2B1A0L
    const val LOOT_COUNT         = 0x05C2B1A8L
    const val LOOT_POS_X         = 0x60L
    const val LOOT_POS_Y         = 0x64L
    const val LOOT_POS_Z         = 0x68L
    const val LOOT_ITEM_TYPE     = 0x70L
}
