package moe.dic1911.autodnd

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import moe.dic1911.autodnd.logging.DNDLogger
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import moe.dic1911.autodnd.data.Storage
import moe.dic1911.autodnd.ui.main.SectionsPagerAdapter
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener


class MainActivity : AppCompatActivity() {
    private lateinit var notiMan: NotificationManager
    private lateinit var fab: FloatingActionButton
    private var restartOnResume = false

    private val TAG = "030"

    private val TAB_TITLES = arrayOf(
        R.string.tab_1_text,
        R.string.tab_2_text,
        R.string.tab_3_text
    )

    private val REQUEST_PERMISSION_RESULT_LISTENER =
        OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            Storage.onShizukuRequestPermissionsResult(
                requestCode,
                grantResult
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)

        Storage.initDNDList(this)

        DNDLogger.logUIOperation("App lists initialized", "DND apps: ${Storage.getAppList(0)?.size}, All apps: ${Storage.getAppList(1)?.size}")

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
        DNDLogger.logUIOperation("MainActivity resumed")

        if (restartOnResume) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        // check if necessary permission granted / service enabled
        Storage.setupStatus = 0
        if (!notiMan.isNotificationPolicyAccessGranted) {
            Storage.setupStatus = 1
            DNDLogger.logPermissionCheck("Notification Policy Access", false)
            Toast.makeText(applicationContext, R.string.notification_policy_tip, Toast.LENGTH_LONG).show()
        }
        if (Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1) {
            val svcs = Settings.Secure.getString(this.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            DNDLogger.logDebug("Setup", "Enabled accessibility services: $svcs")
            if (svcs.contains("moe.dic1911.autodnd.DNDAccessibilityService")) {
                DNDLogger.logPermissionCheck("Accessibility Service", true)
            } else {
                Storage.setupStatus = (Storage.setupStatus or 2)
            }
        } else {
            DNDLogger.logPermissionCheck("Accessibility Service", false)
            Toast.makeText(applicationContext, R.string.accessbility_svc_tip, Toast.LENGTH_LONG).show()
            Storage.setupStatus = (Storage.setupStatus or 2)
        }

        Storage.setupStatus = Storage.setupStatus
        Storage.checkPermission(1)
        if (Storage.setupStatus == 0) {
            fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorAccent))
            fab.setImageIcon(Icon.createWithResource(baseContext, R.drawable.ic_baseline_refresh_32))
        } else {
            fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.design_default_color_error))
            fab.setImageIcon(Icon.createWithResource(baseContext, android.R.drawable.stat_sys_warning))

            if (Storage.setupStatus == 2 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val dlgBuilder = AlertDialog.Builder(this)
                val noteTxt = TextView(this)
                noteTxt.setText(R.string.note_content)
                noteTxt.setPadding(30, 40, 30, 0)
                dlgBuilder.setTitle(R.string.note)
                dlgBuilder.setView(noteTxt)
                dlgBuilder.setPositiveButton("OK") { dialog, which ->
                    runSetupStep()
                }
                dlgBuilder.create().show()
            }
        }

        fab.setOnClickListener(fun(it: View) {
            runSetupStep()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }

    private fun runSetupStep() {
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
                restartOnResume = true
                Toast.makeText(applicationContext, R.string.accessbility_svc_tip, Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                if (settingsIntent !is Activity) {
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(settingsIntent)
            }
        }
    }

}