package ted.gun0912.clustering

import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.clustering.ClusterManager
import ted.gun0912.clustering.clustering.algo.ScreenBasedAlgorithm

abstract class TedClustering<Clustering, C : TedClusterItem, RealMarker, Marker : TedMarker<ImageDescriptor>, Map, ImageDescriptor>(
    private val clusterManager: ClusterManager<Clustering, C, RealMarker, Marker, Map, ImageDescriptor>
) {

    fun clearItems() = internalChangeItem { clusterManager.clearItems() }

    fun addItems(items: Collection<C>) = internalChangeItem { clusterManager.addItems(items) }

    fun addItem(myItem: C) = internalChangeItem { clusterManager.addItem(myItem) }

    fun removeItem(item: C) = internalChangeItem { clusterManager.removeItem(item) }


    private fun internalChangeItem(action: (() -> Unit)) {
        action.invoke()
        clusterManager.cluster()
    }

    fun setAlgorithm(algorithm: ScreenBasedAlgorithm<C>) = clusterManager.setAlgorithm(algorithm)


}