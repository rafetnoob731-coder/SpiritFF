package com.spirit.ff.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.spirit.ff.IUserService
import com.spirit.ff.R
import com.spirit.ff.engine.*
import com.spirit.ff.memory.MemoryReader
import com.spirit.ff.memory.MemoryWriter
import com.spirit.ff.util.SoundManager

class FloatingOverlayService : Service() {

    companion object {
        var shizukuService: IUserService? = null
        private const val CHANNEL_ID = "spirit_ff"
    }

    private lateinit var wm: WindowManager
    private lateinit var panelView: View
    private lateinit var sound: SoundManager

    private var mem: MemoryReader? = null
    private var wrt: MemoryWriter? = null
    private var aimbot:      AimbotEngine?      = null
    private var esp:         EspEngine?          = null
    private var tpEngine:    TeleportEngine?     = null
    private var fastSniper:  FastSniperEngine?   = null
    private var magicBullet: MagicBulletEngine?  = null
    private var auraKill:    AuraKillEngine?     = null

    private val handler = Handler(Looper.getMainLooper())
    private val gameCheck = object : Runnable {
        override fun run() {
            val m = mem
            if (m != null && !m.alive()) {
                val ok = m.init()
                updateStatus(ok)
                if (!ok) stopAll()
            }
            tvEspDebug?.text = "esp: ${EspData.debug}"
            handler.postDelayed(this, 2000)
        }
    }

    // View refs
    private var tvStatus:         TextView? = null
    private var tvEspDebug:       TextView? = null
    private var statusDot:        View?     = null
    private var switchAimbot:     Switch?   = null
    private var switchEsp:        Switch?   = null
    private var switchFastSniper: Switch?   = null
    private var switchMagicBullet:Switch?   = null
    private var switchAuraKill:   Switch?   = null

    override fun onCreate() {
        super.onCreate()
        sound = SoundManager(this)
        createChannel()
        startForeground(1, buildNotif())
        initEngines()
        buildPanel()
        handler.post(gameCheck)
    }

    private fun initEngines() {
        val svc = shizukuService ?: return
        val m   = MemoryReader(svc).also { mem = it }
        val ok  = m.init()
        val w   = MemoryWriter(svc, m.pid).also { wrt = it }
        aimbot      = AimbotEngine(m, w)
        esp         = EspEngine(m)
        tpEngine    = TeleportEngine(m, w)
        fastSniper  = FastSniperEngine(m, w)
        magicBullet = MagicBulletEngine(m, w)
        auraKill    = AuraKillEngine(m, w)
        updateStatus(ok)
    }

    private fun buildPanel() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 300 }

        panelView = LayoutInflater.from(this).inflate(R.layout.floating_panel, null)

        tvStatus          = panelView.findViewById(R.id.tvStatus)
        tvEspDebug        = panelView.findViewById(R.id.tvEspDebug)
        statusDot         = panelView.findViewById(R.id.statusDot)
        switchAimbot      = panelView.findViewById(R.id.switchAimbot)
        switchEsp         = panelView.findViewById(R.id.switchEsp)
        switchFastSniper  = panelView.findViewById(R.id.switchFastSniper)
        switchMagicBullet = panelView.findViewById(R.id.switchMagicBullet)
        switchAuraKill    = panelView.findViewById(R.id.switchAuraKill)

        switchAimbot?.setOnCheckedChangeListener      { _, on -> toggle(on, aimbot) }
        switchEsp?.setOnCheckedChangeListener          { _, on -> toggle(on, esp) }
        switchFastSniper?.setOnCheckedChangeListener   { _, on -> toggle(on, fastSniper) }
        switchMagicBullet?.setOnCheckedChangeListener  { _, on -> toggle(on, magicBullet) }
        switchAuraKill?.setOnCheckedChangeListener     { _, on -> toggle(on, auraKill) }

        panelView.findViewById<android.widget.Button>(R.id.btnTeleport)
            .setOnClickListener { tpEngine?.teleportToNearestLoot() }

        // drag
        var ix = 0; var iy = 0; var itx = 0f; var ity = 0f
        panelView.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    ix = params.x; iy = params.y; itx = ev.rawX; ity = ev.rawY; false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = ix + (ev.rawX - itx).toInt()
                    params.y = iy + (ev.rawY - ity).toInt()
                    wm.updateViewLayout(panelView, params); true
                }
                else -> false
            }
        }

        wm.addView(panelView, params)
    }

    private fun toggle(on: Boolean, eng: Any?) {
        if (eng == null) { initEngines(); return }
        if (on) {
            sound.playActivate()
            when (eng) {
                is AimbotEngine      -> eng.start()
                is EspEngine         -> eng.start()
                is FastSniperEngine  -> eng.start()
                is MagicBulletEngine -> eng.start()
                is AuraKillEngine    -> eng.start()
            }
        } else {
            sound.playDeactivate()
            when (eng) {
                is AimbotEngine      -> eng.stop()
                is EspEngine         -> eng.stop()
                is FastSniperEngine  -> eng.stop()
                is MagicBulletEngine -> eng.stop()
                is AuraKillEngine    -> eng.stop()
            }
        }
    }

    private fun updateStatus(gameRunning: Boolean) {
        handler.post {
            if (gameRunning) {
                tvStatus?.text = "Free Fire connected ✓"
                tvStatus?.setTextColor(0xFF2ECC71.toInt())
                statusDot?.setBackgroundColor(0xFF2ECC71.toInt())
            } else {
                tvStatus?.text = "Waiting for Free Fire..."
                tvStatus?.setTextColor(0xFFF59E0B.toInt())
                statusDot?.setBackgroundColor(0xFFEF4444.toInt())
            }
        }
    }

    private fun stopAll() {
        aimbot?.stop();      switchAimbot?.isChecked      = false
        esp?.stop();          switchEsp?.isChecked          = false
        fastSniper?.stop();   switchFastSniper?.isChecked   = false
        magicBullet?.stop();  switchMagicBullet?.isChecked  = false
        auraKill?.stop();     switchAuraKill?.isChecked     = false
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopAll()
        sound.release()
        if (::panelView.isInitialized) runCatching { wm.removeView(panelView) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SpiritFF Overlay", NotificationManager.IMPORTANCE_LOW)
            )
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SpiritFF")
        .setContentText("Overlay active")
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .build()
}
