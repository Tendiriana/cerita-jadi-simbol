package id.taktik.xmod

import android.content.ComponentName
import android.graphics.Bitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    var icon: Bitmap?
)
