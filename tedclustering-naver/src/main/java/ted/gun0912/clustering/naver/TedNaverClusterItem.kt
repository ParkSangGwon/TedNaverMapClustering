package ted.gun0912.clustering.naver

import com.naver.maps.geometry.LatLng
import ted.gun0912.clustering.clustering.ClusterItem
import ted.gun0912.clustering.geometry.TedLatLng

abstract class TedNaverClusterItem : ClusterItem {
    abstract fun getLatLng(): LatLng

    override fun getTedLatLng(): TedLatLng {
        val latLng = getLatLng()
        return TedLatLng(latLng.latitude, latLng.longitude)
    }
}