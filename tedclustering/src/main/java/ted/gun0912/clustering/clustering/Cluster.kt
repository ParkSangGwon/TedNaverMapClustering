package ted.gun0912.clustering.clustering

import ted.gun0912.clustering.geometry.TedLatLng


interface Cluster<T : TedClusterItem> {
    val position: TedLatLng

    val items: Collection<T>

    val size: Int
}