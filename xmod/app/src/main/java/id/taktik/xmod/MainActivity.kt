package id.taktik.xmod

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.os.VibrationEffect
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var accent = 0xFFE8B24A.toInt()
    private val ID = Locale("id", "ID")
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var prefs: android.content.SharedPreferences
    private val allApps = ArrayList<AppInfo>()
    private val hidden = HashSet<String>()
    private val history = ArrayList<String>()
    private var notFound = 0
    private var vaultMode = false
    private var chargeAnim = 0
    private lateinit var adapter: AppAdapter

    private var lastRx = 0L
    private var cursorOn = true
    private var typeJob: Runnable? = null

    // views
    private lateinit var photo: ImageView
    private lateinit var glitch: GlitchView
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
    private lateinit var sysLegend: TextView
    private lateinit var netLegend: TextView
    private lateinit var logLegend: TextView
    private lateinit var promptDollar: TextView
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
    private lateinit var drawerTitle: TextView
    private lateinit var optOverlay: View
    private lateinit var bootOverlay: View
    private lateinit var bootText: TextView
    private lateinit var termOverlay: View
    private lateinit var termTitle: TextView
    private lateinit var termOut: TextView

    private val PREF_PHOTO = "photo_uri"
    private val PREF_MODE = "photo_mode"
    private val PREF_THEME = "theme"
    private val PREF_HIDDEN = "hidden"
    private val PREF_BOOT = "boot"
    private val PICK_PHOTO = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("xmod", Context.MODE_PRIVATE)
        accent = themeColor()
        loadHidden()
        hideSystemBars()
        requestPhonePermission()

        bind()
        setupPhoto()
        setupHeader()
        setupCommand()
        setupDock()
        setupDrawer()
        setupOptimize()
        applyAccentColors()

        tick()
        ui.post(loop)
        loadApps()
        showBoot()
    }

    private fun bind() {
        photo = findViewById(R.id.photo)
        glitch = findViewById(R.id.glitch)
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
        sysLegend = findViewById(R.id.sys_legend)
        netLegend = findViewById(R.id.net_legend)
        logLegend = findViewById(R.id.log_legend)
        promptDollar = findViewById(R.id.prompt_dollar)
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
        drawerTitle = findViewById(R.id.drawer_title)
        optOverlay = findViewById(R.id.opt_overlay)
        bootOverlay = findViewById(R.id.boot_overlay)
        bootText = findViewById(R.id.boot_text)
        termOverlay = findViewById(R.id.term_overlay)
        termTitle = findViewById(R.id.term_title)
        termOut = findViewById(R.id.term_out)

        hostLine.text = getString(R.string.host_line)
        cmdLine.text = getString(R.string.cmd_line)
        legendHeader.text = "[ x-mod v" + BuildConfig.VERSION_NAME + " ]"
        legendSession.text = "[ session:active ]"
        netPeak.text = "peak —"
    }

    // ---------- Tema / warna aksen ----------
    private fun themeColor(): Int = when (prefs.getString(PREF_THEME, "amber")) {
        "green" -> 0xFF46D17A.toInt()
        "red" -> 0xFFE5484D.toInt()
        else -> 0xFFE8B24A.toInt()
    }

    private fun applyAccentColors() {
        val c = accent
        for (tv in listOf(legendHeader, sysLegend, netLegend, logLegend, seconds, promptDollar, drawerTitle)) {
            tv.setTextColor(c)
        }
        findViewById<TextView>(R.id.opt_btn).setTextColor(c)
        logCursor.setBackgroundColor(c)
        cmdBlock.setBackgroundColor(c)
        findViewById<HudBracketsView>(R.id.photo_brackets).setAccent(c)
        spark.setAccent(c)
        glitch.tint = c
    }

    private fun setTheme(name: String) {
        val t = when {
            name.startsWith("gr") || name.contains("matrix") -> "green"
            name.startsWith("re") -> "red"
            name.startsWith("am") || name.startsWith("gol") || name.isEmpty() -> "amber"
            else -> null
        }
        if (t == null) { openTerminal("theme", "pakai: theme amber | green | red"); return }
        prefs.edit().putString(PREF_THEME, t).apply()
        accent = themeColor()
        applyAccentColors()
        setupDock()
        loadApps()
        glitch.trigger()
        toast("tema: $t")
    }

    // ---------- Getar halus ----------
    private fun haptic(ms: Long = 14) {
        try {
            val vib = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            vib.vibrate(VibrationEffect.createOneShot(ms, 70))
        } catch (e: Exception) { }
    }

    // ---------- Efek ketik ----------
    private fun typeText(tv: TextView, full: String, cps: Int, onDone: (() -> Unit)? = null) {
        typeJob?.let { ui.removeCallbacks(it) }
        val step = (1000L / cps).coerceAtLeast(8L)
        var i = 0
        val r = object : Runnable {
            override fun run() {
                i += 1
                tv.text = full.substring(0, i.coerceAtMost(full.length))
                (tv.parent as? ScrollView)?.post { (tv.parent as ScrollView).fullScroll(View.FOCUS_DOWN) }
                if (i < full.length) ui.postDelayed(this, step) else { typeJob = null; onDone?.invoke() }
            }
        }
        typeJob = r
        ui.post(r)
    }

    // ---------- Boot sequence ----------
    private fun showBoot() {
        if (prefs.getString(PREF_BOOT, "1") != "1") { bootOverlay.visibility = View.GONE; return }
        bootOverlay.visibility = View.VISIBLE
        bootOverlay.alpha = 1f
        val full = listOf(
            "x-mod bootloader v" + BuildConfig.VERSION_NAME,
            "",
            "[  ok  ] mount /system",
            "[  ok  ] mount /home",
            "[  ok  ] decrypt vault ... ok",
            "[  ok  ] load launcher modules",
            "[  ok  ] index applications",
            "[  ok  ] net link up",
            "[  ok  ] session start",
            "",
            "> welcome, operator"
        ).joinToString("\n")
        typeText(bootText, full, 90) { ui.postDelayed({ hideBoot() }, 450) }
        bootOverlay.setOnClickListener {
            typeJob?.let { r -> ui.removeCallbacks(r) }
            bootText.text = full
            hideBoot()
        }
    }

    private fun hideBoot() {
        haptic()
        glitch.trigger()
        bootOverlay.animate().alpha(0f).setDuration(220)
            .withEndAction { bootOverlay.visibility = View.GONE }.start()
    }

    // ---------- Terminal output ----------
    private fun openTerminal(title: String, body: String) {
        haptic(); glitch.trigger()
        termTitle.text = "[ x-mod :: $title ]"
        termOverlay.visibility = View.VISIBLE
        typeText(termOut, body, 160)
        termOverlay.setOnClickListener { closeTerminal() }
    }

    private fun closeTerminal() {
        typeJob?.let { ui.removeCallbacks(it) }
        termOverlay.visibility = View.GONE
    }

    // ---------- Foto ----------
    private fun setupPhoto() {
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
        } catch (e: Exception) { toast("Tidak bisa membuka galeri") }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK_PHOTO && res == RESULT_OK) {
            val uri = data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            prefs.edit().putString(PREF_PHOTO, uri.toString()).apply()
            applyPhoto()
            toast("wallpaper diganti")
        }
    }

    // ---------- Header & gesture ----------
    private fun setupHeader() {
        hostLine.setOnLongClickListener { openDrawer(false); true }
        cmdLine.setOnLongClickListener { openDrawer(false); true }
        legendSession.setOnLongClickListener { haptic(); openSettings(Settings.ACTION_HOME_SETTINGS); true }
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

    // ---------- Prompt + saran + riwayat ----------
    private fun setupCommand() {
        cmdInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { buildSuggestions(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        cmdInput.onFocusChangeListener = View.OnFocusChangeListener { _, has ->
            findViewById<View>(R.id.dock).visibility = if (has) View.GONE else View.VISIBLE
            if (has) buildSuggestions(cmdInput.text?.toString() ?: "") else suggestScroll.visibility = View.GONE
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

        if (q.isEmpty()) {
            if (cmdInput.hasFocus() && history.isNotEmpty()) {
                suggestScroll.visibility = View.VISIBLE
                for (cmd in history.take(12)) {
                    addChip("↺ $cmd") { cmdInput.setText(cmd); cmdInput.setSelection(cmd.length) }
                }
            } else suggestScroll.visibility = View.GONE
            return
        }

        val out = ArrayList<Pair<String, () -> Unit>>()
        val kw = listOf<Pair<String, () -> Unit>>(
            "apps" to { openDrawer(false) }, "optimize" to { startOptimize() },
            "foto" to { pickPhoto() }, "neofetch" to { openTerminal("neofetch", neofetch()) },
            "help" to { openTerminal("help", helpText()) }, "ps" to { openTerminal("ps", psText()) },
            "hide apps" to { openDrawer(true) }, "theme" to { openTerminal("theme", "theme amber | green | red") },
            "default" to { openSettings(Settings.ACTION_HOME_SETTINGS) }
        )
        for ((k, act) in kw) if (k.startsWith(q)) out.add("·$k" to act)
        val starts = allApps.filter { !isHidden(it.packageName) && it.label.lowercase(ID).startsWith(q) }
        val contains = allApps.filter { val l = it.label.lowercase(ID); !isHidden(it.packageName) && l.contains(q) && !l.startsWith(q) }
        for (a in starts) out.add(a.label to { launchApp(a) })
        for (a in contains) out.add(a.label to { launchApp(a) })

        if (out.isEmpty()) { suggestScroll.visibility = View.GONE; return }
        firstSuggestion = out[0].second
        suggestScroll.visibility = View.VISIBLE
        for ((label, act) in out.take(14)) addChip(label, act)
    }

    private fun addChip(label: String, act: () -> Unit) {
        val chip = TextView(this)
        chip.text = label
        chip.setTextColor(accent)
        chip.textSize = 12f
        chip.typeface = android.graphics.Typeface.MONOSPACE
        chip.maxLines = 1
        chip.background = getDrawable(R.drawable.chip_bg)
        chip.setPadding(dp(12), dp(7), dp(12), dp(7))
        val lp = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(8)
        chip.layoutParams = lp
        chip.setOnClickListener {
            haptic(); act()
            if (label.startsWith("↺ ")) { /* isi ulang, tetap fokus */ }
            else { cmdInput.setText(""); hideKeyboard(cmdInput); cmdInput.clearFocus(); buildSuggestions("") }
        }
        suggestRow.addView(chip)
    }

    private fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()

    private fun runCommand(raw: String) {
        val c = raw.trim()
        if (c.isEmpty()) return
        history.remove(c); history.add(0, c); if (history.size > 20) history.removeAt(history.size - 1)
        haptic()
        if (handleCommand(c.lowercase(ID), c)) { notFound = 0; return }
        val lc = c.lowercase(ID)
        val hit = allApps.firstOrNull { !isHidden(it.packageName) && it.label.lowercase(ID).contains(lc) }
        if (hit != null) { launchApp(hit); notFound = 0; return }
        notFound++
        if (notFound >= 3) { notFound = 0; openTerminal("alert", intrusionText()) }
        else pushLog("sh    command not found: $c")
    }

    private fun handleCommand(lc: String, c: String): Boolean {
        when {
            lc == "apps" || lc == "app" || lc == "ls" || lc == "drawer" || lc == "menu" -> openDrawer(false)
            lc == "hide apps" || lc == "vault" || lc == ".vault" -> openDrawer(true)
            lc.startsWith("hide ") -> hideApp(c.substring(5))
            lc.startsWith("unhide ") -> unhideApp(c.substring(7))
            lc.startsWith("theme") -> setTheme(lc.removePrefix("theme").trim())
            lc == "boot on" -> { prefs.edit().putString(PREF_BOOT, "1").apply(); toast("boot: on") }
            lc == "boot off" -> { prefs.edit().putString(PREF_BOOT, "0").apply(); toast("boot: off") }
            lc == "boot" -> showBoot()
            lc == "help" || lc == "?" -> openTerminal("help", helpText())
            lc == "neofetch" || lc == "fetch" -> openTerminal("neofetch", neofetch())
            lc == "ps" || lc == "top" -> openTerminal("ps", psText())
            lc == "whoami" -> openTerminal("whoami", "operator@s24")
            lc == "date" || lc == "time" -> openTerminal("date", dateFull())
            lc == "uname" || lc == "ver" || lc == "version" -> openTerminal("uname", unameText())
            lc.startsWith("echo ") -> openTerminal("echo", c.substring(5))
            lc == "clear" || lc == "cls" || lc == "exit" -> closeTerminal()
            lc == "opt" || lc == "optimize" || lc == "clean" || lc == "boost" -> startOptimize()
            lc == "foto" || lc == "wallpaper" || lc == "img" || lc == "bg" || lc == "wp" -> pickPhoto()
            lc == "wa" || lc == "whatsapp" -> launchPkg("com.whatsapp", "WhatsApp")
            lc == "chrome" || lc == "browser" || lc == "web" -> launchPkg("com.android.chrome", "Chrome")
            lc == "claude" || lc == "ai" -> launchPkg("com.anthropic.claude", "Claude")
            lc == "game" || lc == "gaming" || lc == "hub" -> launchPkg("com.samsung.android.game.gamehome", "Gaming Hub")
            lc == "cam" || lc == "kamera" || lc == "camera" -> openCamera()
            lc == "set" || lc == "setting" || lc == "settings" || lc == "setelan" -> openSettings(Settings.ACTION_SETTINGS)
            lc == "default" || lc == "home" -> openSettings(Settings.ACTION_HOME_SETTINGS)
            else -> return false
        }
        return true
    }

    // ---------- Output perintah ----------
    private fun helpText(): String = listOf(
        "X-MOD SHELL — perintah:", "",
        "  apps / ls       buka laci aplikasi",
        "  hide apps       buka vault (tersembunyi)",
        "  hide <app>      sembunyikan aplikasi",
        "  unhide <app>    tampilkan lagi",
        "  opt             optimize",
        "  foto            ganti wallpaper",
        "  theme <warna>   amber | green | red",
        "  boot on / off   animasi boot",
        "  neofetch        info sistem",
        "  ps              daftar aplikasi",
        "  whoami / date   identitas / waktu",
        "  uname           versi sistem",
        "  clear           tutup terminal",
        "  <nama app>      buka aplikasi"
    ).joinToString("\n")

    private fun neofetch(): String {
        val theme = prefs.getString(PREF_THEME, "amber")
        return listOf(
            "   __   __     operator@s24",
            "   \\ \\ / /     ----------------",
            "    \\ V /      os     : X-Mod " + BuildConfig.VERSION_NAME,
            "    /   \\      host   : Galaxy S24",
            "   /_/ \\_\\     kernel : Android " + Build.VERSION.RELEASE,
            "   x-mod       cpu    : " + cpuInfo(),
            "               ram    : " + memShort(),
            "               disk   : " + stoShort() + " used",
            "               uptime : " + uptime(),
            "               bat    : " + batteryPct() + "%",
            "               apps   : " + allApps.size + " (" + hidden.size + " hidden)",
            "               theme  : " + theme
        ).joinToString("\n")
    }

    private fun psText(): String {
        val sb = StringBuilder("  PID   TASK\n")
        var pid = 1000
        for (a in allApps.filter { !isHidden(it.packageName) }.take(40)) {
            sb.append("  ").append(pid).append("  ").append(a.label).append("\n")
            pid += 7
        }
        return sb.toString().trimEnd()
    }

    private fun unameText(): String =
        "X-Mod " + BuildConfig.VERSION_NAME + " (Android " + Build.VERSION.RELEASE + ") s24 aarch64"

    private fun dateFull(): String = SimpleDateFormat("EEEE, dd MMMM yyyy · HH:mm:ss", ID).format(Date())

    private fun intrusionText(): String = listOf(
        "!! INTRUSION DETECTED !!", "",
        "> unauthorized input logged",
        "> node   : 127.0.0.1",
        "> user   : unknown",
        "> trace  : ████████░░ 80%",
        "> result : access denied", "",
        "> nice try 😏"
    ).joinToString("\n")

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
            } catch (e: Exception) { iv.setImageDrawable(null) }
            iv.setOnClickListener { haptic(); launchPkg(pkg, label) }
        }
    }

    // ---------- Laci aplikasi + vault ----------
    private fun setupDrawer() {
        adapter = AppAdapter(this)
        val grid = findViewById<GridView>(R.id.grid)
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, pos, _ -> launchApp(adapter.getItem(pos)) }
        grid.setOnItemLongClickListener { _, _, pos, _ ->
            val a = adapter.getItem(pos)
            if (vaultMode) { hidden.remove(a.packageName); toast("ditampilkan: ${a.label}") }
            else { hidden.add(a.packageName); toast("disembunyikan: ${a.label}") }
            saveHidden(); haptic()
            filter(findViewById<EditText>(R.id.search).text?.toString() ?: "")
            true
        }
        findViewById<View>(R.id.drawer_back).setOnClickListener { closeDrawer() }
        val search = findViewById<EditText>(R.id.search)
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun openDrawer(vault: Boolean) {
        vaultMode = vault
        drawerTitle.text = if (vault) "~/.vault" else getString(R.string.apps_path)
        drawer.visibility = View.VISIBLE
        haptic(); glitch.trigger()
        filter("")
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
        val base = if (vaultMode) allApps.filter { isHidden(it.packageName) }
        else allApps.filter { !isHidden(it.packageName) }
        val shown = if (query.isEmpty()) base
        else base.filter { it.label.lowercase(ID).contains(query) }
        adapter.setItems(shown)
        val tag = if (vaultMode) ".VAULT" else "ALL APPS"
        findViewById<TextView>(R.id.count).text = "[ $tag · ${shown.size} ]"
    }

    private fun isHidden(pkg: String) = hidden.contains(pkg)
    private fun loadHidden() {
        hidden.clear()
        prefs.getString(PREF_HIDDEN, "")?.split("\n")?.forEach { if (it.isNotBlank()) hidden.add(it) }
    }
    private fun saveHidden() { prefs.edit().putString(PREF_HIDDEN, hidden.joinToString("\n")).apply() }

    private fun hideApp(name: String) {
        val n = name.trim().lowercase(ID)
        val a = allApps.firstOrNull { it.label.lowercase(ID).contains(n) }
        if (a == null) { toast("aplikasi tak ditemukan"); return }
        hidden.add(a.packageName); saveHidden()
        if (drawer.visibility == View.VISIBLE) filter(findViewById<EditText>(R.id.search).text?.toString() ?: "")
        toast("disembunyikan: ${a.label}")
    }

    private fun unhideApp(name: String) {
        val n = name.trim().lowercase(ID)
        val a = allApps.firstOrNull { isHidden(it.packageName) && it.label.lowercase(ID).contains(n) }
        if (a == null) { toast("tidak ada di vault"); return }
        hidden.remove(a.packageName); saveHidden()
        if (drawer.visibility == View.VISIBLE) filter(findViewById<EditText>(R.id.search).text?.toString() ?: "")
        toast("ditampilkan: ${a.label}")
    }

    // ---------- Optimize ----------
    private var optRunning = false
    private fun setupOptimize() {
        findViewById<View>(R.id.opt_close).setOnClickListener { optOverlay.visibility = View.GONE }
        findViewById<View>(R.id.opt_deep).setOnClickListener { openDeviceCare() }
    }

    private fun startOptimize() {
        if (optRunning) return
        optRunning = true
        haptic(); glitch.trigger()
        optOverlay.visibility = View.VISIBLE
        findViewById<View>(R.id.opt_result).visibility = View.GONE
        val title = findViewById<TextView>(R.id.opt_title)
        findViewById<TextView>(R.id.opt_pid).text = "pid 4471"
        val body = findViewById<TextView>(R.id.opt_body)
        val hex = findViewById<TextView>(R.id.opt_hex)
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
                    3 -> sb.append("> mem   " + memLine() + "\n")
                    5 -> sb.append("> sto   " + stoLineText() + "\n")
                    7 -> sb.append("> flush x-mod cache…\n")
                }
                if (step in listOf(2, 4, 6, 8)) sb.append("  scan " + "▓".repeat(on) + "░".repeat(10 - on) + " $pct%\n")
                body.text = sb.toString().trim()
                if (step >= 9) {
                    clearDir(cacheDir)
                    val freed = (before - dirSize(cacheDir)).coerceAtLeast(0)
                    showOptResult(freed)
                    optRunning = false
                } else ui.postDelayed(this, 150)
            }
        }
        ui.postDelayed(run, 150)
    }

    private fun showOptResult(freedBytes: Long) {
        findViewById<View>(R.id.opt_result).visibility = View.VISIBLE
        findViewById<TextView>(R.id.opt_title).text = "[ x-mod :: done ]"
        findViewById<TextView>(R.id.opt_title).alpha = 1f
        haptic(22)
        val mb = freedBytes / (1024.0 * 1024.0)
        findViewById<TextView>(R.id.opt_freed).text =
            (if (mb >= 1) String.format(Locale.US, "%.0f", mb) else "0") + " MB"
        findViewById<TextView>(R.id.opt_detail).text =
            "cache x-mod dibersihkan · RAM bebas " + freeRamGb() + "G\npembersihan dalam → Device Care"
    }

    private fun openDeviceCare() {
        optOverlay.visibility = View.GONE
        try {
            val i = Intent().setComponent(ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity"))
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
        } catch (e: Exception) { openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) }
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

        val type = netType()
        val rx = TrafficStats.getTotalRxBytes()
        val delta = if (lastRx == 0L) 0L else (rx - lastRx).coerceAtLeast(0)
        lastRx = rx
        spark.push((delta / 300000f).coerceIn(0.03f, 1f))

        val bat = batteryPct()
        val charging = batteryCharging()
        val boltL = if (charging) " ⚡" else ""
        statusL.text = hh + ssv
        statusR.text = type.first + "  " + type.second + ":up  " + barAscii(bat, 10) + " " + bat + "%" + boltL

        sysCpu.text = "CPU " + cpuInfo()
        sysMem.text = "MEM " + memLine()
        sysSto.text = "STO " + stoLineText()
        sysNet.text = "NET " + netTraffic(delta)
        if (charging) {
            chargeAnim = (chargeAnim + 1) % 9
            sysBat.text = "BAT " + "▮".repeat(chargeAnim) + "▯".repeat(8 - chargeAnim) + " ⚡" + bat + "%"
        } else {
            sysBat.text = "BAT " + barAscii(bat, 8) + " " + bat + "%"
        }
        sysUp.text = "UP  " + uptime()

        ramLine.text = "RAM " + barAscii(usedRamPct(), 8) + " " + memShort()
        stoLine.text = "STO " + barAscii(usedStoPct(), 8) + " " + stoShort()

        logText.text = buildLog(charging, bat)
    }

    // ---------- Data asli HP ----------
    private fun batteryInfo() = try { registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) } catch (e: Exception) { null }
    private fun batteryPct(): Int = try {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) { 0 }
    private fun batteryCharging(): Boolean = try {
        val s = batteryInfo()?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL
    } catch (e: Exception) { false }
    private fun batteryTemp(): String = try {
        val t = batteryInfo()?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
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
        val mhz = try { File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong() / 1000 } catch (e: Exception) { 0L }
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

    private fun buildLog(charging: Boolean, bat: Int): String {
        val t = now()
        val type = netType()
        val power = if (charging) "charging" else "discharging"
        return t + "  wlan0  link up · " + type.first + "\n" +
               t + "  power  bat " + bat + "%" + batteryTemp() + " · " + power + "\n" +
               t + "  sys    uptime " + uptime()
    }

    // ---------- util ----------
    private fun barAscii(pct: Int, n: Int): String {
        val on = (pct.coerceIn(0, 100) * n / 100)
        return "▮".repeat(on) + "▯".repeat(n - on)
    }
    private fun now() = SimpleDateFormat("HH:mm:ss", ID).format(Date())

    private fun pushLog(line: String) { toast(line.substringAfter("  ").trim()) }

    private fun launchPkg(pkg: String, label: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) }
        else pushLog("sh    " + label + " tidak terpasang")
    }
    private fun launchApp(app: AppInfo) {
        try {
            haptic()
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            i.component = app.component
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            startActivity(i)
        } catch (e: Exception) { toast("Tidak bisa membuka ${app.label}") }
    }
    private fun openCamera() {
        try { startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: ActivityNotFoundException) { pushLog("sh    kamera tidak ada") }
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
            termOverlay.visibility == View.VISIBLE -> closeTerminal()
            optOverlay.visibility == View.VISIBLE -> optOverlay.visibility = View.GONE
            drawer.visibility == View.VISIBLE -> closeDrawer()
            else -> { }
        }
    }
}
