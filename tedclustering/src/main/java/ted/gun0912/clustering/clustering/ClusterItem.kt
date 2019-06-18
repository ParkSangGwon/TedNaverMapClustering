package ted.gun0912.clustering.clustering

import ted.gun0912.clustering.geometry.TedLatLng


interface ClusterItem {
    fun getTedLatLng(): TedLatLng
}