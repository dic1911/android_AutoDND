package moe.dic1911.autodnd.ui.main

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import moe.dic1911.autodnd.R
import moe.dic1911.autodnd.data.AppEntry

@SuppressLint("StaticFieldLeak")
class PageViewModel : ViewModel() {

    private val TOP_TEXT = arrayOf(
        R.string.tab_1_tip,
        R.string.tab_2_tip
    )

    private lateinit var mContext: Context

    private val _index = MutableLiveData<Int>()
    private val _adapter = MutableLiveData<AppListAdapter>()
    val text: LiveData<String> = Transformations.map(_index) {
        mContext.getString(TOP_TEXT[it])
    }

    val applist: LiveData<ArrayList<AppEntry>> = MutableLiveData<ArrayList<AppEntry>>()

    fun passContext(c: Context) {
        mContext = c
    }

    fun updateAdapter(a: ArrayList<AppEntry>) {
        _adapter.value?.appList = a
        _adapter.value?.notifyDataSetChanged()
    }

    fun setAdapter(a: AppListAdapter) {
        _adapter.value = a
    }

    fun getAdapter(): AppListAdapter? {
        return _adapter.value
    }

    fun setIndex(index: Int) {
        _index.value = index
    }

    fun getIndex(): Int? {
        return _index.value
    }
}