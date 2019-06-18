package ted.gun0912.clustering.clustering.algo

import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.clustering.TedClusterItem


interface Algorithm<T : TedClusterItem> {

    val items: Collection<T>

    var maxDistanceBetweenClusteredItems: Int

    fun addItem(item: T)

    fun addItems(items: Collection<T>)

    fun clearItems()

    fun removeItem(item: T)

    fun getClusters(zoom: Double): Set<Cluster<T>>
}