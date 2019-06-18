package ted.gun0912.clustering.naver.demo

import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import ted.gun0912.clustering.naver.TedNaverClustering

class DefaultActivity : BaseDemoActivity() {
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
