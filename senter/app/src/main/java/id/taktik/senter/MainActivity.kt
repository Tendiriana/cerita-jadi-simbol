package id.taktik.senter

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var on = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<android.view.View>(R.id.toggle)
        val status = findViewById<TextView>(R.id.status)
        val icon = findViewById<TextView>(R.id.icon)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = findFlashCamera()

        if (cameraId == null) {
            status.text = getString(R.string.no_flash)
            button.isEnabled = false
            return
        }

        // Jika senter dimatikan sistem saat aplikasi di latar belakang, segarkan tampilan.
        cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    on = enabled
                    render(status, icon, button)
                }
            }
        }, null)

        button.setOnClickListener {
            toggle(status, icon, button)
        }

        render(status, icon, button)
    }

    private fun findFlashCamera(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun toggle(status: TextView, icon: TextView, button: android.view.View) {
        val id = cameraId ?: return
        try {
            on = !on
            cameraManager.setTorchMode(id, on)
            render(status, icon, button)
        } catch (e: Exception) {
            status.text = getString(R.string.error)
        }
    }

    private fun render(status: TextView, icon: TextView, button: android.view.View) {
        if (on) {
            icon.text = "◉"
            status.text = getString(R.string.on)
            button.setBackgroundResource(R.drawable.btn_on)
        } else {
            icon.text = "○"
            status.text = getString(R.string.off)
            button.setBackgroundResource(R.drawable.btn_off)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Matikan senter saat aplikasi ditutup.
        try {
            cameraId?.let { if (on) cameraManager.setTorchMode(it, false) }
        } catch (_: Exception) {
        }
    }
}
