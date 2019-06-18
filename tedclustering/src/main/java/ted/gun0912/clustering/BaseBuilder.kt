package ted.gun0912.clustering

import android.content.Context
import android.view.View
import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.clustering.TedClusterItem

abstract class BaseBuilder<Clustering, C : TedClusterItem, RealMarker, TM : TedMarker<ImageDescriptor>, Map, ImageDescriptor>(
    internal val context: Context,
    internal val map: TedMap<RealMarker, TM, ImageDescriptor>,
    internal var markerClickListener: ((C) -> Unit)? = null,
    internal var clusterClickListener: ((Cluster<C>) -> Unit)? = null,
    internal var clickToCenter: Boolean = true,
    internal var clusterBuckets: IntArray = intArrayOf(10, 20, 50, 100, 200, 500, 1000),
    internal var minClusterSize: Int = 3,
    internal var clusterAnimation: Boolean = true,
    internal var clusterBackground: ((Int) -> Int)? = null,
    internal var clusterText: ((Int) -> String)? = null,
    internal var clusterAddedListener: ((cluster: Cluster<C>, TM) -> Unit)? = null,
    internal var markerAddedListener: ((clusterItem: C, TM) -> Unit)? = null,
    internal var items: Collection<C>? = null,
    internal var item: C? = null,
    private var markerMaker: ((clusterItem: C) -> RealMarker)? = null,
    internal var clusterMaker: ((cluster: Cluster<C>) -> View)? = null
) {

    fun customCluster(clusterMaker: ((cluster: Cluster<C>) -> View)) =
        apply { this.clusterMaker = clusterMaker }

    fun customMarker(markerMaker: ((clusterItem: C) -> RealMarker)? = null) =
        apply { this.markerMaker = markerMaker }

    fun item(item: C) =
        apply { this.item = item }

    fun items(items: Collection<C>) =
        apply { this.items = items }

    fun getMarker(clusterItem: C): TM {
        return (markerMaker?.invoke(clusterItem)?.let {
            map.getMarker(it)
        } ?: map.getMarker())
            .also {
                it.position = clusterItem.getTedLatLng()
            }
    }

    fun markerAddedListener(listener: ((clusterItem: C, TM) -> Unit)) =
        apply { this.markerAddedListener = listener }

    fun clusterAddedListener(listener: ((cluster: Cluster<C>, TM) -> Unit)) =
        apply { this.clusterAddedListener = listener }

    fun clusterText(action: ((Int) -> String)) =
        apply { this.clusterText = action }

    fun clusterBackground(action: ((Int) -> Int)) =
        apply { this.clusterBackground = action }

    fun clusterAnimation(animate: Boolean) =
        apply { this.clusterAnimation = animate }

    fun minClusterSize(size: Int) =
        apply { this.minClusterSize = size }

    fun markerClickListener(listener: ((C) -> Unit)) =
        apply { this.markerClickListener = listener }

    fun clusterClickListener(listener: ((Cluster<C>) -> Unit)) =
        apply { this.clusterClickListener = listener }

    fun clickToCenter(animate: Boolean) =
        apply { this.clickToCenter = animate }

    fun clusterBuckets(clusterBuckets: IntArray) =
        apply { this.clusterBuckets = clusterBuckets }


    abstract fun make(): Clustering

}