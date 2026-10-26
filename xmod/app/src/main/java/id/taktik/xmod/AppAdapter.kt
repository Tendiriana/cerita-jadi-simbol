package id.taktik.xmod

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AppAdapter(private val context: Context) : BaseAdapter() {

    private val items = ArrayList<AppInfo>()

    fun setItems(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): AppInfo = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_app, parent, false)

        val app = items[position]
        val icon = view.findViewById<ImageView>(R.id.app_icon)
        val label = view.findViewById<TextView>(R.id.app_label)

        if (app.icon != null) {
            icon.setImageBitmap(app.icon)
        } else {
            icon.setImageDrawable(null)
        }
        label.text = app.label

        return view
    }
}
