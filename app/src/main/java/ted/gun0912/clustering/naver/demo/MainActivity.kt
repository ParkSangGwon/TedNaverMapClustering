package ted.gun0912.clustering.naver.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startActivity(R.id.btn_java, JavaActivity::class.java)
        startActivity(R.id.btn_default, DefaultActivity::class.java)
        startActivity(R.id.btn_add, AddItemActivity::class.java)
        startActivity(R.id.btn_custom_marker_cluster, CustomMarkerClusterActivity::class.java)
        startActivity(R.id.btn_click_listener, ClickListenerActivity::class.java)
    }


    private fun startActivity(@IdRes btnResId: Int, clazz: Class<*>) {
        findViewById<Button>(btnResId).setOnClickListener {
            startActivity(Intent(this, clazz))
        }
    }
}
