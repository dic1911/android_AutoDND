package moe.dic1911.autodnd.ui.main

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.recyclerview.widget.RecyclerView
import moe.dic1911.autodnd.R
import moe.dic1911.autodnd.data.AppEntry
import moe.dic1911.autodnd.data.Storage
import moe.dic1911.autodnd.logging.DNDLogger

class AppEntryHolder (itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val ic_app: ImageView = itemView.findViewById(R.id.app_ic)
    private val txt_appname: TextView = itemView.findViewById(R.id.app_hname)
    private val txt_pkgname: TextView = itemView.findViewById(R.id.app_pkgname)

    fun fillValue(e: AppEntry) {
        ic_app.setImageDrawable(e.ic_app)
        txt_appname.text = e.app_hname
        txt_pkgname.text = e.app_pkgname
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val pkg = txt_pkgname.text.toString()
        val added = Storage.setAutoDNDApp(pkg)
        val action = if (added) "Added to" else "Removed from"
        DNDLogger.logUIOperation("App selection", "$action DND list: $pkg")

        if (added)
            Toast.makeText(v!!.context, R.string.added, LENGTH_SHORT).show()
        else
            Toast.makeText(v!!.context, R.string.removed, LENGTH_SHORT).show()
    }
}