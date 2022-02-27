package moe.dic1911.autodnd.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.dic1911.autodnd.R
import moe.dic1911.autodnd.data.Storage
import moe.dic1911.autodnd.databinding.FragmentMainBinding
import moe.dic1911.autodnd.databinding.FragmentMainBinding.inflate

class MainFragment(val index: Int) : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageViewModel = ViewModelProvider(this).get((PageViewModel::class.java)).apply {
            setIndex(index)
            setAdapter(AppListAdapter())
            passContext(requireActivity().applicationContext)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val textView: TextView = root.findViewById(R.id.section_label)
        val recyclerView: RecyclerView = root.findViewById(R.id.app_list_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = AppListAdapter()
        Log.d("030_lst", "${pageViewModel.getIndex()}, size = ${Storage.getAppList(pageViewModel.getIndex()!!)!!.size}")
        adapter.appList = Storage.getAppList(pageViewModel.getIndex()!!)!!
        recyclerView.adapter = adapter
        pageViewModel.text.observe(viewLifecycleOwner) {
            Log.d("030.txt", it.toString())
            textView.text = it
        }

        Storage.initialized.observe(viewLifecycleOwner) {
            root.findViewById<ProgressBar>(R.id.progress).visibility = when (it) {
                true -> View.GONE
                else -> View.VISIBLE
            }
            recyclerView.visibility = when (it) {
                true -> View.VISIBLE
                else -> View.GONE
            }
        }

        pageViewModel.applist.observe(viewLifecycleOwner) {
            Log.d("030-list", it.size.toString())
            (recyclerView.adapter as AppListAdapter?)?.appList = it
            (recyclerView.adapter as AppListAdapter?)?.notifyDataSetChanged()
        }
        if (Storage.prefs_str.hasObservers()) {
            Storage.prefs_str.removeObservers(this)
        }
        Storage.prefs_str.observe(viewLifecycleOwner) {
            if (pageViewModel.getIndex() == 0) {
                Log.d("030-list", it.size.toString())
                (recyclerView.adapter as AppListAdapter?)?.appList = Storage.getAppList(0)!!
                (recyclerView.adapter as AppListAdapter?)?.notifyDataSetChanged()
            }
        }

        _binding = inflate(inflater, container, false)

        return root
    }

    override fun onResume() {
        super.onResume()
        val sb = StringBuilder()
        val txt = binding.warning
        when (Storage.setupStatus) {
            0 -> txt.visibility = View.GONE
            1 -> {
                txt.visibility = View.VISIBLE
                sb.append(getString(R.string.setup_tip)).append("\n")
                sb.append(getString(R.string.notification_policy_tip))
            }
            2 -> {
                txt.visibility = View.VISIBLE
                sb.append(getString(R.string.setup_tip)).append("\n")
                sb.append(getString(R.string.accessbility_svc_tip))
            }
            3 -> {
                txt.visibility = View.VISIBLE
                sb.append(getString(R.string.setup_tip)).append("\n")
                sb.append(getString(R.string.notification_policy_tip)).append("\n")
                sb.append(getString(R.string.accessbility_svc_tip))
            }
        }
        txt.text = sb.toString()
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int, mContext: Context): MainFragment {
            return MainFragment(sectionNumber).apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}