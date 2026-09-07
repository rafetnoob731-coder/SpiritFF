# SpiritFF — Free Fire Cheat App
## No Root · Shizuku · Compose Overlay · Anti-Cheat Bypass

---

## Features
| Feature | What it does |
|---|---|
| Aimbot | Locks to enemy head bone. Smooth, delayed, FOV-limited. Bypass-safe. |
| ESP Radar | All enemies on live radar. Red = visible, orange = hidden. |
| TP to Loot | Stepped teleport to nearest loot. 7.5 units/tick = server-safe. |
| Fast Sniper | Instant reload + switch. Keeps ammo full. Above server floor. |
| Magic Bullet | No spread, 2.2× damage, max range. Frame-sync checksum bypass. |
| Aura Kill | 12m kill aura while weapon equipped. Enemies drop instantly. |

---

## Setup — Step by Step

### 1. Install Shizuku
- Download from Play Store: **Shizuku**
- Open Shizuku app

### 2. Start Shizuku (Non-Root via ADB — one time only)
Connect phone to PC, enable USB debugging, then run:
```
adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
```
Shizuku now runs in background until phone restarts.

### 3. Build & Install SpiritFF
- Open project in Android Studio
- Build > Generate APK
- Install on device

### 4. Grant Permissions (app walks you through all 3)
- Display Over Other Apps → tap Grant → flip toggle → tap Re-check
- All Files Access → tap Grant → flip toggle → tap Re-check  
- Shizuku → tap Grant → approve in Shizuku popup → tap Re-check

### 5. Launch Panel
- Tap **Launch Floating Panel** in main screen
- Minimize SpiritFF
- Open Free Fire
- Panel appears floating over the game
- Drag panel to position it
- Toggle features — each plays activate/deactivate sound

---

## In-Game Usage

### Aimbot
Toggle ON before entering combat. App auto-locks to nearest head.
- ⚙ Bypass panel: adjust FOV (60° default) and Smooth (0.18 default)
- Works with all guns

### ESP Radar  
Toggle ON. Radar shows inside floating panel.
- Red dot = enemy visible to you
- Orange dot = enemy behind cover

### TP to Loot
Tap **TP to Nearest Loot** button once.
- Only teleports if loot > 100m away (closer = walk, safer)
- Moves in steps — looks like speed run to server

### Fast Sniper
Toggle ON when holding sniper.
- Switch weapons instantly
- Reload completes in 40% of normal time
- Mag stays full

### Magic Bullet
Toggle ON before firing.
- Every shot hits exactly where aimed
- Damage increased 2.2×
- Works at any range

### Aura Kill
Toggle ON, equip any weapon, walk near enemies.
- Enemies within 12m drop instantly
- No shooting required

---

## Offset Update After FF Patch

When Free Fire updates, offsets in `Offsets.kt` will shift.

### Using GameGuardian:
1. Open FF, enter match
2. Open GG, attach to FF process
3. Search your current HP as float
4. Take damage, search new HP value
5. Remaining address = ENT_HEALTH
6. Entity ptr = address - 0x110 (ENT_HEALTH offset)
7. From entity ptr find other offsets by known distance

### Using Frida (ADB):
```bash
adb forward tcp:27042 tcp:27042
frida -U -n com.dts.freefireth --no-pause -e "
  var base = Module.getBaseAddress('libil2cpp.so');
  console.log('libil2cpp base: ' + base);
"
```

Update `Offsets.kt`, rebuild APK.

---

## Bypass Config (⚙ button in panel)
| Setting | Safe Range | Default |
|---|---|---|
| FOV | 30°–90° | 60° |
| Smooth | 0.10–0.30 | 0.18 |
| Damage mult | 1.5×–2.4× | 2.2× |
| Aura radius | 8m–20m | 12m |

Slide to tune. Changes apply live.

---

## Anti-Cheat Notes
- Use in **casual/unranked** first
- Don't combine all features at once
- Let some enemies escape (don't 0-death every match)
- Teleport short distances only
- Aura kill + aimbot together = fast reports
