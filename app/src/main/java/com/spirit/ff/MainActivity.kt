package com.spirit.ff

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.spirit.ff.service.FloatingOverlayService
import com.spirit.ff.service.ShizukuUserService
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var tvOverlay:    TextView
    private lateinit var tvStorage:    TextView
    private lateinit var tvShizuku:    TextView
    private lateinit var tvGameStatus: TextView
    private lateinit var btnLaunch:    Button
    private lateinit var btnStop:      Button

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunner = object : Runnable {
        override fun run() { updatePermissions(); handler.postDelayed(this, 1500) }
    }

    private val shizukuListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> updatePermissions() }

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(packageName, ShizukuUserService::class.java.name)
    ).daemon(false).processNameSuffix("service").version(1)

    private val shizukuConn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            FloatingOverlayService.shizukuService = IUserService.Stub.asInterface(b)
            updateGameStatus()
        }
        override fun onServiceDisconnected(n: ComponentName) {
            FloatingOverlayService.shizukuService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOverlay    = findViewById(R.id.tvPermOverlay)
        tvStorage    = findViewById(R.id.tvPermStorage)
        tvShizuku    = findViewById(R.id.tvPermShizuku)
        tvGameStatus = findViewById(R.id.tvGameStatus)
        btnLaunch    = findViewById(R.id.btnLaunch)
        btnStop      = findViewById(R.id.btnStop)

        Shizuku.addRequestPermissionResultListener(shizukuListener)

        tvOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        tvStorage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        }
        tvShizuku.setOnClickListener {
            try {
                if (!Shizuku.pingBinder()) {
                    packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        ?.let { startActivity(it) }
                } else {
                    Shizuku.requestPermission(100)
                }
            } catch (_: Exception) {}
        }

        btnLaunch.setOnClickListener {
            if (allGranted()) {
                bindShizuku()
                startForegroundService(Intent(this, FloatingOverlayService::class.java))
                btnLaunch.visibility = View.GONE
                btnStop.visibility   = View.VISIBLE
            } else {
                Toast.makeText(this, "Grant all permissions first", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, FloatingOverlayService::class.java))
            btnLaunch.visibility = View.VISIBLE
            btnStop.visibility   = View.GONE
        }

        handler.post(checkRunner)
    }

    private fun bindShizuku() {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Shizuku.bindUserService(serviceArgs, shizukuConn)
            }
        } catch (_: Exception) {}
    }

    private fun updatePermissions() {
        val overlay = Settings.canDrawOverlays(this)
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            android.os.Environment.isExternalStorageManager() else true
        val shizuku = try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }

        fun set(tv: TextView, ok: Boolean, label: String) {
            if (ok) { tv.text = "✓ $label"; tv.setTextColor(0xFF2ECC71.toInt()) }
            else    { tv.text = "✗ $label — tap to grant"; tv.setTextColor(0xFFEF4444.toInt()) }
        }
        set(tvOverlay, overlay, "Overlay")
        set(tvStorage, storage, "Storage")
        set(tvShizuku, shizuku, "Shizuku")
        updateGameStatus()
    }

    private fun updateGameStatus() {
        Thread {
            val svc = FloatingOverlayService.shizukuService
            val running = if (svc != null) {
                val pid = try { svc.getPidByPackage("com.dts.freefireth")
                    .takeIf { it > 0 } ?: svc.getPidByPackage("com.dts.freefiremax") } catch (_: Exception) { -1 }
                (pid ?: -1) > 0
            } else false
            runOnUiThread {
                if (running) {
                    tvGameStatus.text = "● Free Fire running"
                    tvGameStatus.setTextColor(0xFF2ECC71.toInt())
                } else {
                    tvGameStatus.text = "● Free Fire not detected"
                    tvGameStatus.setTextColor(0xFFF59E0B.toInt())
                }
            }
        }.start()
    }

    private fun allGranted(): Boolean {
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            android.os.Environment.isExternalStorageManager() else true
        val shizuku = try {
            Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
        return Settings.canDrawOverlays(this) && storage && shizuku
    }

    override fun onResume() { super.onResume(); updatePermissions() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        runCatching { Shizuku.unbindUserService(serviceArgs, shizukuConn, true) }
        super.onDestroy()
    }
}
