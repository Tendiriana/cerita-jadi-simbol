package id.taktik.xmod

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val accent = 0xFFE8B24A.toInt()

    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var sysText: TextView
    private lateinit var countText: TextView
    private lateinit var searchEdit: EditText
    private lateinit var grid: GridView
    private lateinit var adapter: AppAdapter

    private val allApps = ArrayList<AppInfo>()

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { updateClock() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clock)
        dateText = findViewById(R.id.date)
        sysText = findViewById(R.id.sys)
        countText = findViewById(R.id.count)
        searchEdit = findViewById(R.id.search)
        grid = findViewById(R.id.grid)

        adapter = AppAdapter(this)
        grid.adapter = adapter

        grid.setOnItemClickListener { _, _, position, _ ->
            launchApp(adapter.getItem(position))
        }

        findViewById<TextView>(R.id.set_default).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
            }
        }

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        updateClock()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(timeReceiver, filter)
        updateClock()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(timeReceiver) } catch (_: Exception) {}
    }

    private fun updateClock() {
        val now = Date()
        val id = Locale("id", "ID")
        clockText.text = SimpleDateFormat("HH:mm", id).format(now)
        dateText.text = SimpleDateFormat("EEEE, dd MMM yyyy", id).format(now)
        val bat = try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) { -1 }
        sysText.text = if (bat in 0..100) "> net stabil · bat $bat%" else "> net stabil"
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val iconPx = (resources.displayMetrics.density * 46f).toInt().coerceIn(72, 160)
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(query, 0)
            val list = ArrayList<AppInfo>()
            for (ri in resolved) {
                try {
                    val label = ri.loadLabel(pm).toString()
                    val pkg = ri.activityInfo.packageName
                    if (pkg == packageName) continue
                    val comp = ComponentName(pkg, ri.activityInfo.name)
                    val icon = try {
                        IconRenderer.render(ri.loadIcon(pm), iconPx, accent)
                    } catch (e: Exception) { null }
                    list.add(AppInfo(label, pkg, comp, icon))
                } catch (e: Exception) { /* lewati aplikasi bermasalah */ }
            }
            list.sortBy { it.label.lowercase(Locale.getDefault()) }
            runOnUiThread {
                allApps.clear()
                allApps.addAll(list)
                applyFilter(searchEdit.text?.toString() ?: "")
            }
        }.start()
    }

    private fun applyFilter(q: String) {
        val query = q.trim().lowercase(Locale.getDefault())
        val shown = if (query.isEmpty()) allApps
        else allApps.filter { it.label.lowercase(Locale.getDefault()).contains(query) }
        adapter.setItems(shown)
        countText.text = "[ ALL APPS · ${shown.size} ]"
    }

    private fun launchApp(app: AppInfo) {
        try {
            val i = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
            i.component = app.component
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa membuka ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Launcher tetap di home saat tombol kembali ditekan")
    override fun onBackPressed() {
        // Jangan keluar dari launcher.
    }
}
