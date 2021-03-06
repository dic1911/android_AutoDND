package moe.dic1911.autodnd

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import moe.dic1911.autodnd.data.AppEntry
import moe.dic1911.autodnd.data.Storage
import moe.dic1911.autodnd.ui.main.MainFragment
import moe.dic1911.autodnd.ui.main.SectionsPagerAdapter


class MainActivity : AppCompatActivity() {
    private lateinit var notiMan: NotificationManager
    private lateinit var fab: FloatingActionButton

    private val TAG = "030"

    private val TAB_TITLES = arrayOf(
        R.string.tab_1_text,
        R.string.tab_2_text
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Storage.initDNDList(this)

        Log.d("030_pkg", "applist.size=${Storage.getAppList(0)?.size}")
        Log.d("030_pkg", "applist_dnd.size=${Storage.getAppList(1)?.size}")

        val sectionsPagerAdapter = SectionsPagerAdapter(this)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = getString(TAB_TITLES[position])
        }.attach()


        fab = findViewById(R.id.fab)
        notiMan = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
        // check if necessary permission granted / service enabled
        Storage.setupStatus = 0
        if (!notiMan.isNotificationPolicyAccessGranted) {
            Storage.setupStatus = 1
            Log.d(TAG, "notification policy access not granted")
            Toast.makeText(applicationContext, R.string.notification_policy_tip, Toast.LENGTH_LONG).show()
        }
        if (Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1) {
            val svcs = Settings.Secure.getString(this.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            Log.d(TAG, svcs)
            if (svcs.contains("moe.dic1911.autodnd.DNDAccessibilityService")) {
                Log.d(TAG, "Auto DND enabled")
            } else {
                Storage.setupStatus = (Storage.setupStatus or 2)
            }
        } else {
            Log.d(TAG, "No enabled services")
            Toast.makeText(applicationContext, R.string.accessbility_svc_tip, Toast.LENGTH_LONG).show()
            Storage.setupStatus = (Storage.setupStatus or 2)
        }

        Storage.setupStatus = Storage.setupStatus

        if (Storage.setupStatus == 0) {
            fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorAccent))
            fab.setImageIcon(Icon.createWithResource(baseContext, R.drawable.ic_baseline_refresh_32))
        } else {
            fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.design_default_color_error))
            fab.setImageIcon(Icon.createWithResource(baseContext, android.R.drawable.stat_sys_warning))
        }

        fab.setOnClickListener(fun(it: View) {
            when (Storage.setupStatus) {
                0 -> {
                    Toast.makeText(this, R.string.refreshing, Toast.LENGTH_LONG).show()
                    Storage.initialized.postValue(false)
                    Storage.initDNDList(this)
                }
                1,3 -> {
                    Toast.makeText(applicationContext, R.string.notification_policy_tip, Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
                2 -> {
                    Toast.makeText(applicationContext, R.string.accessbility_svc_tip, Toast.LENGTH_LONG).show()
                    val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    if (settingsIntent !is Activity) {
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(settingsIntent)
                }
            }
        })
    }
}