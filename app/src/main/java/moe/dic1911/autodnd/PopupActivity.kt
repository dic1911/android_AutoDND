package moe.dic1911.autodnd

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class PopupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popup)
        finishAffinity()
    }
}