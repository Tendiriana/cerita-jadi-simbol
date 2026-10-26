package id.taktik.xmod

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val accent = 0xFFE8B24A.toInt()
    private val ID = Locale("id", "ID")
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var prefs: android.content.SharedPreferences
    private val allApps = ArrayList<AppInfo>()
    private lateinit var adapter: AppAdapter

    private var lastRx = 0L
    private var cursorOn = true

    // views
    private lateinit var photo: ImageView
    private lateinit var statusL: TextView
    private lateinit var statusR: TextView
    private lateinit var hostLine: TextView
    private lateinit var cmdLine: TextView
    private lateinit var clock: TextView
    private lateinit var seconds: TextView
    private lateinit var dateLine: TextView
    private lateinit var ramLine: TextView
    private lateinit var stoLine: TextView
    private lateinit var legendHeader: TextView
    private lateinit var legendSession: TextView
    private lateinit var sysCpu: TextView
    private lateinit var sysMem: TextView
    private lateinit var sysSto: TextView
    private lateinit var sysNet: TextView
    private lateinit var sysBat: TextView
    private lateinit var sysUp: TextView
    private lateinit var spark: SparklineView
    private lateinit var netPeak: TextView
    private lateinit var logText: TextView
    private lateinit var logCursor: View
    private lateinit var cmdInput: EditText
    private lateinit var cmdBlock: View
    private lateinit var suggestScroll: View
    private lateinit var suggestRow: android.widget.LinearLayout
    private var firstSuggestion: (() -> Unit)? = null
    private lateinit var drawer: View
    private lateinit var optOverlay: View

    private val PREF_PHOTO = "photo_uri"
    private val PREF_MODE = "photo_mode"
    private val PICK_PHOTO = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("xmod", Context.MODE_PRIVATE)
        hideSystemBars()
        requestPhonePermission()

        bind()
        setupPhoto()
        setupHeader()
        setupCommand()
        setupDock()
        setupDrawer()
        setupOptimize()

        tick()
        ui.post(loop)
        loadApps()
    }

    private fun bind() {
        photo = findViewById(R.id.photo)
        statusL = findViewById(R.id.status_left)
        statusR = findViewById(R.id.status_right)
        hostLine = findViewById(R.id.host_line)
        cmdLine = findViewById(R.id.cmd_line)
        clock = findViewById(R.id.clock)
        seconds = findViewById(R.id.seconds)
        dateLine = findViewById(R.id.date_line)
        ramLine = findViewById(R.id.ram_line)
        stoLine = findViewById(R.id.sto_line)
        legendHeader = findViewById(R.id.legend_header)
        legendSession = findViewById(R.id.legend_session)
        sysCpu = findViewById(R.id.sys_cpu)
        sysMem = findViewById(R.id.sys_mem)
        sysSto = findViewById(R.id.sys_sto)
        sysNet = findViewById(R.id.sys_net)
        sysBat = findViewById(R.id.sys_bat)
        sysUp = findViewById(R.id.sys_up)
        spark = findViewById(R.id.spark)
        netPeak = findViewById(R.id.net_peak)
        logText = findViewById(R.id.log_text)
        logCursor = findViewById(R.id.log_cursor)
        cmdInput = findViewById(R.id.cmd_input)
        cmdBlock = findViewById(R.id.cmd_block)
        suggestScroll = findViewById(R.id.suggest_scroll)
        suggestRow = findViewById(R.id.suggest_row)
        drawer = findViewById(R.id.drawer)
        optOverlay = findViewById(R.id.opt_overlay)

        hostLine.text = getString(R.string.host_line)
        cmdLine.text = getString(R.string.cmd_line)
        legendHeader.text = "[ x-mod v" + BuildConfig.VERSION_NAME + " ]"
        legendSession.text = "[ session:active ]"
        netPeak.text = "peak —"
    }

    // ---------- Foto ----------
    private fun setupPhoto() {
        // gradient fade ke hitam
        val fade = findViewById<View>(R.id.photo_fade)
        val g = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF0A0A0B.toInt(), 0x590A0A0B, 0x1A0A0A0B, 0x8C0A0A0B.toInt(), 0xEB0A0A0B.toInt())
        )
        fade.background = g
        applyPhoto()
    }

    private fun applyPhoto() {
        val mode = prefs.getString(PREF_MODE, "amber")
        val cm = ColorMatrix()
        if (mode == "asli") {
            cm.setScale(0.72f, 0.72f, 0.72f, 1f)
        } else {
            cm.setSaturation(0f)
            val tint = ColorMatrix(floatArrayOf(
                0.80f, 0f, 0f, 0f, 24f,
                0f, 0.62f, 0f, 0f, 8f,
                0f, 0f, 0.30f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(tint)
        }
        photo.colorFilter = ColorMatrixColorFilter(cm)
        photo.scaleType = ImageView.ScaleType.CENTER_CROP

        val uriStr = prefs.getString(PREF_PHOTO, null)
        val bmp = if (uriStr != null) loadBitmap(Uri.parse(uriStr)) else null
        if (bmp != null) photo.setImageBitmap(bmp) else photo.setImageDrawable(null)
    }

    private fun loadBitmap(uri: Uri): android.graphics.Bitmap? {
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            val reqW = resources.displayMetrics.widthPixels.coerceAtLeast(720)
            val reqH = (resources.displayMetrics.density * 440f).toInt()
            var sample = 1
            while (o.outWidth / sample > reqW * 2 || o.outHeight / sample > reqH * 2) sample *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
        } catch (e: Exception) { null }
    }

    private fun pickPhoto() {
        try {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(i, PICK_PHOTO)
        } catch (e: Exception) {
            toast("Tidak bisa membuka galeri")
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK_PHOTO && res == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { /* abaikan */ }
            prefs.edit().putString(PREF_PHOTO, uri.toString()).apply()
            applyPhoto()
            pushLog(now() + " xmod  wallpaper diganti")
        }
    }

    // ---------- Header & gesture tersembunyi ----------
    private fun setupHeader() {
        // tekan lama baris host → buka laci aplikasi
        hostLine.setOnLongClickListener { openDrawer(); true }
        cmdLine.setOnLongClickListener { openDrawer(); true }

        // pintasan tersembunyi: tekan lama [ session:active ] → pengaturan launcher default
        legendSession.setOnLongClickListener { openSettings(Settings.ACTION_HOME_SETTINGS); true }

        findViewById<View>(R.id.opt_btn).setOnClickListener { startOptimize() }
    }

    private fun hideSystemBars() {
        try {
            val c = WindowInsetsControllerCompat(window, window.decorView)
            c.hide(WindowInsetsCompat.Type.statusBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) { }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun requestPhonePermission() {
        try {
            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 9)
            }
        } catch (e: Exception) { }
    }

    // ---------- Prompt tersembunyi ----------
    private fun setupCommand() {
        cmdInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { buildSuggestions(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        // saat kolom perintah fokus: sembunyikan dock biar menempel rapi di atas keyboard
        cmdInput.onFocusChangeListener = View.OnFocusChangeListener { _, has ->
            findViewById<View>(R.id.dock).visibility = if (has) View.GONE else View.VISIBLE
            if (!has) suggestScroll.visibility = View.GONE
        }
        cmdInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val f = firstSuggestion
                if (f != null) f.invoke() else runCommand(cmdInput.text.toString())
                cmdInput.setText("")
                hideKeyboard(cmdInput)
                cmdInput.clearFocus()
                buildSuggestions("")
                true
            } else false
        }
    }

    private fun buildSuggestions(q0: String) {
        suggestRow.removeAllViews()
        firstSuggestion = null
        val q = q0.trim().lowercase(ID)
        if (q.isEmpty()) { suggestScroll.visibility = View.GONE; return }

        val out = ArrayList<Pair<String, () -> Unit>>()
        // perintah bawaan
        val kw = listOf<Pair<String, () -> Unit>>(
            "apps" to { openDrawer() },
            "optimize" to { startOptimize() },
            "foto" to { pickPhoto() },
            "setelan" to { openSettings(Settings.ACTION_SETTINGS) },
            "default" to { openSettings(Settings.ACTION_HOME_SETTINGS) }
        )
        for ((k, act) in kw) if (k.startsWith(q)) out.add("·$k" to act)
        // aplikasi: yang diawali query dulu, lalu yang mengandung
        val starts = allApps.filter { it.label.lowercase(ID).startsWith(q) }
        val contains = allApps.filter { val l = it.label.lowercase(ID); l.contains(q) && !l.startsWith(q) }
        for (a in starts) out.add(a.label to { launchApp(a) })
        for (a in contains) out.add(a.label to { launchApp(a) })

        if (out.isEmpty()) { suggestScroll.visibility = View.GONE; return }
        firstSuggestion = out[0].second
        suggestScroll.visibility = View.VISIBLE

        val padH = dp(12); val padV = dp(7); val gap = dp(8)
        for ((label, act) in out.take(14)) {
            val chip = TextView(this)
            chip.text = label
            chip.setTextColor(accent)
            chip.textSize = 12f
            chip.typeface = android.graphics.Typeface.MONOSPACE
            chip.maxLines = 1
            chip.background = getDrawable(R.drawable.chip_bg)
            chip.setPadding(padH, padV, padH, padV)
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = gap
            chip.layoutParams = lp
            chip.setOnClickListener {
                act(); cmdInput.setText(""); hideKeyboard(cmdInput); cmdInput.clearFocus(); buildSuggestions("")
            }
            suggestRow.addView(chip)
        }
    }

    private fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()

    private fun runCommand(raw: String) {
        val c = raw.trim().lowercase(ID)
        if (c.isEmpty()) return
        when (c) {
            "apps", "app", "drawer", "ls", "menu" -> openDrawer()
            "opt", "optimize", "clean", "boost" -> startOptimize()
            "foto", "wallpaper", "img", "bg", "wp" -> pickPhoto()
            "wa", "whatsapp" -> launchPkg("com.whatsapp", "WhatsApp")
            "chrome", "browser", "web" -> launchPkg("com.android.chrome", "Chrome")
            "claude", "ai" -> launchPkg("com.anthropic.claude", "Claude")
            "game", "gaming", "hub" -> launchPkg("com.samsung.android.game.gamehome", "Gaming Hub")
            "cam", "kamera", "camera" -> openCamera()
            "set", "setting", "settings", "setelan" -> openSettings(Settings.ACTION_SETTINGS)
            "default", "home" -> openSettings(Settings.ACTION_HOME_SETTINGS)
            "help", "?" -> pushLog(now() + " xmod  perintah rahasia aktif")
            else -> {
                // coba cocokkan nama aplikasi
                val hit = allApps.firstOrNull { it.label.lowercase(ID).contains(c) }
                if (hit != null) launchApp(hit)
                else pushLog(now() + " sh    command not found: " + c)
            }
        }
    }

    // ---------- Dock ----------
    private val dockApps = listOf(
        Triple(R.id.dock_0, "com.whatsapp", "WhatsApp"),
        Triple(R.id.dock_1, "com.android.chrome", "Chrome"),
        Triple(R.id.dock_2, "com.anthropic.claude", "Claude"),
        Triple(R.id.dock_3, "com.samsung.android.game.gamehome", "Gaming Hub")
    )

    private fun setupDock() {
        val px = (resources.displayMetrics.density * 30f).toInt()
        for ((id, pkg, label) in dockApps) {
            val iv = findViewById<ImageView>(id)
            try {
                val d = packageManager.getApplicationIcon(pkg)
                iv.setImageBitmap(IconRenderer.render(d, px, accent))
            } catch (e: Exception) {
                iv.setImageDrawable(null)
            }
            iv.setOnClickListener { launchPkg(pkg, label) }
        }
    }

    // ---------- Laci aplikasi ----------
    private fun setupDrawer() {
        adapter = AppAdapter(this)
        val grid = findViewById<GridView>(R.id.grid)
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, pos, _ -> launchApp(adapter.getItem(pos)) }

        findViewById<View>(R.id.drawer_back).setOnClickListener { closeDrawer() }

        val search = findViewById<EditText>(R.id.search)
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun openDrawer() {
        drawer.visibility = View.VISIBLE
    }

    private fun closeDrawer() {
        drawer.visibility = View.GONE
        val search = findViewById<EditText>(R.id.search)
        search.setText("")
        hideKeyboard(search)
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val px = (resources.displayMetrics.density * 46f).toInt().coerceIn(72, 160)
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = ArrayList<AppInfo>()
            for (ri in pm.queryIntentActivities(q, 0)) {
                try {
                    val pkg = ri.activityInfo.packageName
                    if (pkg == packageName) continue
                    val comp = ComponentName(pkg, ri.activityInfo.name)
                    val icon = try { IconRenderer.render(ri.loadIcon(pm), px, accent) } catch (e: Exception) { null }
                    list.add(AppInfo(ri.loadLabel(pm).toString(), pkg, comp, icon))
                } catch (e: Exception) { }
            }
            list.sortBy { it.label.lowercase(ID) }
            runOnUiThread {
                allApps.clear(); allApps.addAll(list)
                filter((findViewById<EditText>(R.id.search)).text?.toString() ?: "")
            }
        }.start()
    }

    private fun filter(q: String) {
        val query = q.trim().lowercase(ID)
        val shown = if (query.isEmpty()) allApps
        else allApps.filter { it.label.lowercase(ID).contains(query) }
        adapter.setItems(shown)
        findViewById<TextView>(R.id.count).text = "[ ALL APPS · ${shown.size} ]"
    }

    // ---------- Optimize (jujur: animasi + serahkan ke Device Care) ----------
    private var optRunning = false
    private fun setupOptimize() {
        findViewById<View>(R.id.opt_close).setOnClickListener { optOverlay.visibility = View.GONE }
        findViewById<View>(R.id.opt_deep).setOnClickListener { openDeviceCare() }
    }

    private fun startOptimize() {
        if (optRunning) return
        optRunning = true
        optOverlay.visibility = View.VISIBLE
        findViewById<View>(R.id.opt_result).visibility = View.GONE
        val title = findViewById<TextView>(R.id.opt_title)
        val pid = findViewById<TextView>(R.id.opt_pid)
        val body = findViewById<TextView>(R.id.opt_body)
        val hex = findViewById<TextView>(R.id.opt_hex)
        pid.text = "pid 4471"

        val before = dirSize(cacheDir)
        val sb = StringBuilder("$ sudo x-mod --scan\n")
        var step = 0
        val run = object : Runnable {
            override fun run() {
                step++
                title.text = if (step < 7) "[ x-mod :: scanning ]" else "[ x-mod :: flushing ]"
                title.alpha = if (step % 2 == 0) 1f else 0.5f
                val sbh = StringBuilder()
                for (i in 0 until 9) sbh.append("0x" + Integer.toHexString((step * 131 + i * 977) and 0xffffff).padStart(6, '0') + "\n")
                hex.text = sbh.toString()
                val pct = (step * 12).coerceAtMost(100)
                val on = pct / 10
                when (step) {
                    1 -> sb.append("> scan  " + "▓".repeat(on) + "░".repeat(10 - on) + " $pct%\n")
                    3 -> { sb.append("> mem   " + memLine() + "\n") }
                    5 -> { sb.append("> sto   " + stoLineText() + "\n") }
                    7 -> sb.append("> flush x-mod cache…\n")
                    else -> if (step <= 9) sb.setCharAt(0, '$') // no-op keep
                }
                if (step in listOf(2, 4, 6, 8)) sb.append("  scan " + "▓".repeat(on) + "░".repeat(10 - on) + " $pct%\n")
                body.text = sb.toString().trim()

                if (step >= 9) {
                    // bersihkan cache milik X-Mod sendiri (jujur, kecil)
                    clearDir(cacheDir)
                    val freed = (before - dirSize(cacheDir)).coerceAtLeast(0)
                    showOptResult(freed)
                    optRunning = false
                } else {
                    ui.postDelayed(this, 150)
                }
            }
        }
        ui.postDelayed(run, 150)
    }

    private fun showOptResult(freedBytes: Long) {
        findViewById<View>(R.id.opt_result).visibility = View.VISIBLE
        findViewById<TextView>(R.id.opt_title).text = "[ x-mod :: done ]"
        findViewById<TextView>(R.id.opt_title).alpha = 1f
        val mb = freedBytes / (1024.0 * 1024.0)
        findViewById<TextView>(R.id.opt_freed).text =
            (if (mb >= 1) String.format(Locale.US, "%.0f", mb) else "0") + " MB"
        findViewById<TextView>(R.id.opt_detail).text =
            "cache x-mod dibersihkan · RAM bebas " + freeRamGb() + "G\npembersihan dalam → Device Care"
        pushLog(now() + " xmod  optimize · cache flushed")
    }

    private fun openDeviceCare() {
        optOverlay.visibility = View.GONE
        // Samsung Device Care; fallback ke setelan penyimpanan.
        try {
            val i = Intent().setComponent(ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity"))
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
        } catch (e: Exception) {
            openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        }
    }

    // ---------- Loop live ----------
    private val loop = object : Runnable {
        override fun run() {
            tick()
            cursorOn = !cursorOn
            logCursor.visibility = if (cursorOn) View.VISIBLE else View.INVISIBLE
            cmdBlock.visibility = if (cmdInput.hasFocus()) View.INVISIBLE else (if (cursorOn) View.VISIBLE else View.INVISIBLE)
            ui.postDelayed(this, 1000)
        }
    }

    private fun tick() {
        val d = Date()
        val hh = SimpleDateFormat("HH:mm", ID).format(d)
        val ssv = ":" + SimpleDateFormat("ss", ID).format(d)
        clock.text = hh
        seconds.text = ssv
        val days = arrayOf("MIN", "SEN", "SEL", "RAB", "KAM", "JUM", "SAB")
        val cal = java.util.Calendar.getInstance()
        dateLine.text = days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1] + " " +
            SimpleDateFormat("dd.MM.yyyy", ID).format(d) + " · W" +
            String.format(Locale.US, "%02d", cal.get(java.util.Calendar.WEEK_OF_YEAR))

        // net type + traffic
        val type = netType()
        val rx = TrafficStats.getTotalRxBytes()
        val delta = if (lastRx == 0L) 0L else (rx - lastRx).coerceAtLeast(0)
        lastRx = rx
        spark.push((delta / 300000f).coerceIn(0.03f, 1f))

        val bat = batteryPct()
        statusL.text = hh + ssv
        statusR.text = type.first + "  " + type.second + ":up  " + barAscii(bat, 10) + " " + bat + "%"

        // sys panel
        sysCpu.text = "CPU " + cpuInfo()
        sysMem.text = "MEM " + memLine()
        sysSto.text = "STO " + stoLineText()
        sysNet.text = "NET " + netTraffic(delta)
        sysBat.text = "BAT " + barAscii(bat, 8) + " " + bat + "%"
        sysUp.text = "UP  " + uptime()

        ramLine.text = "RAM " + barAscii(usedRamPct(), 8) + " " + memShort()
        stoLine.text = "STO " + barAscii(usedStoPct(), 8) + " " + stoShort()

        logText.text = buildLog()
    }

    // ---------- Data asli HP ----------
    private fun batteryPct(): Int = try {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) { 0 }

    private fun batteryTemp(): String = try {
        val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val t = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (t > 0) " " + (t / 10) + "°C" else ""
    } catch (e: Exception) { "" }

    private fun memInfo(): ActivityManager.MemoryInfo {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo(); am.getMemoryInfo(mi); return mi
    }
    private fun usedRamPct(): Int { val mi = memInfo(); return (((mi.totalMem - mi.availMem).toDouble() / mi.totalMem) * 100).toInt() }
    private fun freeRamGb(): String { val mi = memInfo(); return String.format(Locale.US, "%.1f", mi.availMem / 1.0e9) }
    private fun memLine(): String { val mi = memInfo(); return barAscii(usedRamPct(), 8) + " " + String.format(Locale.US, "%.1f/%.0fG", (mi.totalMem - mi.availMem) / 1.0e9, mi.totalMem / 1.0e9) }
    private fun memShort(): String { val mi = memInfo(); return String.format(Locale.US, "%.1f/%.0fG", (mi.totalMem - mi.availMem) / 1.0e9, mi.totalMem / 1.0e9) }

    private fun statfs(): StatFs = StatFs(Environment.getDataDirectory().path)
    private fun usedStoPct(): Int { val s = statfs(); val t = s.totalBytes; val a = s.availableBytes; return if (t > 0) (((t - a).toDouble() / t) * 100).toInt() else 0 }
    private fun stoLineText(): String { val s = statfs(); return barAscii(usedStoPct(), 8) + " " + String.format(Locale.US, "%.0f/%.0fG", (s.totalBytes - s.availableBytes) / 1.0e9, s.totalBytes / 1.0e9) }
    private fun stoShort(): String { val s = statfs(); return String.format(Locale.US, "%.0fG", (s.totalBytes - s.availableBytes) / 1.0e9) }

    private fun cpuInfo(): String {
        val cores = Runtime.getRuntime().availableProcessors()
        val mhz = try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong() / 1000
        } catch (e: Exception) { 0L }
        return if (mhz > 0) cores.toString() + "×" + String.format(Locale.US, "%.1fGHz", mhz / 1000.0) else cores.toString() + " core"
    }

    private fun netType(): Pair<String, String> {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps == null -> "OFF" to "net"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI" to "wlan0"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellGen() to "rmnet0"
                else -> "NET" to "net0"
            }
        } catch (e: Exception) { "NET" to "net0" }
    }

    private fun cellGen(): String {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val t = if (Build.VERSION.SDK_INT >= 24) tm.dataNetworkType else @Suppress("DEPRECATION") tm.networkType
            when (t) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
                else -> "LTE"
            }
        } catch (e: SecurityException) { "CELL" } catch (e: Exception) { "CELL" }
    }
    private fun netTraffic(delta: Long): String {
        val kb = delta / 1024.0
        return if (kb >= 1024) String.format(Locale.US, "↓%.1fM", kb / 1024) else String.format(Locale.US, "↓%.0fK", kb)
    }

    private fun uptime(): String {
        var s = SystemClock.elapsedRealtime() / 1000
        val h = s / 3600; s %= 3600; val m = s / 60; val sec = s % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
    }

    private fun buildLog(): String {
        val t = now()
        val type = netType()
        return t + "  wlan0  link up · " + type.first + "\n" +
               t + "  power  bat " + batteryPct() + "%" + batteryTemp() + "\n" +
               t + "  sys    uptime " + uptime()
    }

    // ---------- util ----------
    private fun barAscii(pct: Int, n: Int): String {
        val on = (pct.coerceIn(0, 100) * n / 100)
        return "▮".repeat(on) + "▯".repeat(n - on)
    }
    private fun now() = SimpleDateFormat("HH:mm:ss", ID).format(Date())

    private val logQueue = ArrayList<String>()
    private fun pushLog(line: String) {
        logQueue.add(0, line)
        while (logQueue.size > 1) logQueue.removeAt(logQueue.size - 1)
        toast(line.substringAfter("  ").trim())
    }

    private fun launchPkg(pkg: String, label: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) }
        else pushLog(now() + " sh    " + label + " tidak terpasang")
    }
    private fun launchApp(app: AppInfo) {
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            i.component = app.component
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            startActivity(i)
        } catch (e: Exception) { toast("Tidak bisa membuka ${app.label}") }
    }
    private fun openCamera() {
        try { startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: ActivityNotFoundException) { pushLog(now() + " sh    kamera tidak ada") }
    }
    private fun openSettings(action: String) {
        try { startActivity(Intent(action)) } catch (e: Exception) { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {} }
    }

    private fun dirSize(f: File): Long = try {
        if (f.isFile) f.length() else (f.listFiles()?.sumOf { dirSize(it) } ?: 0L)
    } catch (e: Exception) { 0L }
    private fun clearDir(f: File) { try { f.listFiles()?.forEach { if (it.isDirectory) clearDir(it); it.delete() } } catch (e: Exception) {} }

    private fun hideKeyboard(v: View) {
        try { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0) } catch (e: Exception) {}
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onResume() { super.onResume(); applyPhoto(); tick() }
    override fun onDestroy() { super.onDestroy(); ui.removeCallbacksAndMessages(null) }

    @Deprecated("Launcher tetap di home")
    override fun onBackPressed() {
        when {
            optOverlay.visibility == View.VISIBLE -> optOverlay.visibility = View.GONE
            drawer.visibility == View.VISIBLE -> closeDrawer()
            else -> { /* diam */ }
        }
    }
}
