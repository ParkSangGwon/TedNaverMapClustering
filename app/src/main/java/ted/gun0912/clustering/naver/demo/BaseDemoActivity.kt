package ted.gun0912.clustering.naver.demo

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.map.MapFragment
import com.naver.maps.map.OnMapReadyCallback

abstract class BaseDemoActivity(@LayoutRes private val layoutId: Int = R.layout.activity_map) :
    AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        setUpMap()
    }

    private fun setUpMap() {

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: run {
                MapFragment.newInstance().also {
                    supportFragmentManager.beginTransaction().add(R.id.map_fragment, it).commit()
                }
            }
        mapFragment.getMapAsync(this)
    }


    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

}