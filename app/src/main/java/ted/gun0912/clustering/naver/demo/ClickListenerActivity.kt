package ted.gun0912.clustering.naver.demo

import android.widget.Toast
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import ted.gun0912.clustering.naver.TedNaverClustering

class ClickListenerActivity : BaseDemoActivity() {
    lateinit var naverMap: NaverMap

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

        TedNaverClustering.with<NaverItem>(this, naverMap)
            .items(getItems())
            .markerClickListener { naverItem ->
                val position = naverItem.position
                Toast.makeText(
                    this,
                    "${position.latitude},${position.longitude} 클릭됨",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .clusterClickListener { cluster ->
                val position = cluster.position
                Toast.makeText(
                    this,
                    "${cluster.size}개 클러스터 ${position.latitude},${position.longitude} 클릭됨",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .make()


    }

    private fun getItems(): List<NaverItem> {
        val bounds = naverMap.contentBounds
        return ArrayList<NaverItem>().apply {
            repeat(50) {
                val temp = NaverItem(
                    (bounds.northLatitude - bounds.southLatitude) * Math.random() + bounds.southLatitude,
                    (bounds.eastLongitude - bounds.westLongitude) * Math.random() + bounds.westLongitude
                )
                add(temp)
            }
        }

    }
}
