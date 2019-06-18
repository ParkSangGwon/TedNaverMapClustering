package ted.gun0912.clustering.naver.demo

import android.widget.Button
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import ted.gun0912.clustering.naver.TedNaverClustering

class AddItemActivity : BaseDemoActivity(R.layout.activity_add_item) {
    lateinit var naverMap: NaverMap
    lateinit var tedNaverClustering: TedNaverClustering<NaverItem>

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.moveCamera(
            CameraUpdate.toCameraPosition(
                CameraPosition(
                    NaverMap.DEFAULT_CAMERA_POSITION.target,
                    NaverMap.DEFAULT_CAMERA_POSITION.zoom
                )
            )
        )
        tedNaverClustering = TedNaverClustering.with<NaverItem>(this, naverMap)
            .make()

        findViewById<Button>(R.id.btn_add).setOnClickListener {
            val naverItem = getRandomItem()
            tedNaverClustering.addItem(naverItem)
            val cameraUpdate = CameraUpdate.scrollTo(
                LatLng(
                    naverItem.position.latitude,
                    naverItem.position.longitude
                )
            )
            naverMap.moveCamera(cameraUpdate)
        }

        findViewById<Button>(R.id.btn_remove_all).setOnClickListener {
            tedNaverClustering.clearItems()
        }


    }

    private fun getRandomItem(): NaverItem {
        val bounds = naverMap.contentBounds
        return NaverItem(
            (bounds.northLatitude - bounds.southLatitude) * Math.random() + bounds.southLatitude,
            (bounds.eastLongitude - bounds.westLongitude) * Math.random() + bounds.westLongitude
        )
    }
}
