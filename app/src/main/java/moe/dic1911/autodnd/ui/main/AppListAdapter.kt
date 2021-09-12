package moe.dic1911.autodnd.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.dic1911.autodnd.R
import moe.dic1911.autodnd.data.AppEntry

class AppListAdapter : RecyclerView.Adapter<AppEntryHolder>() {

    var appList: ArrayList<AppEntry> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppEntryHolder {
        val entry = LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false)
        return AppEntryHolder(entry)
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    override fun onBindViewHolder(holder: AppEntryHolder, position: Int) {
        // update when scrolling, unused for now
        val entry = appList[position]
        holder.fillValue(entry)
    }
}