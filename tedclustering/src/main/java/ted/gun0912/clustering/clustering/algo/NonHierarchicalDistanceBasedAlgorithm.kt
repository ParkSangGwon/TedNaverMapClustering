package ted.gun0912.clustering.clustering.algo

import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.geometry.Bounds
import ted.gun0912.clustering.geometry.Point
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.projection.SphericalMercatorProjection
import ted.gun0912.clustering.quadtree.PointQuadTree


/**
 * A simple clustering algorithm with O(nlog n) performance. Resulting clusters are not
 * hierarchical.
 * <p/>
 * High level algorithm:<br>
 * 1. Iterate over items in the order they were added (candidate clusters).<br>
 * 2. Create a cluster with the center of the item. <br>
 * 3. Add all items that are within a certain distance to the cluster. <br>
 * 4. Move any items out of an existing cluster if they are closer to another cluster. <br>
 * 5. Remove those items from the list of candidate clusters.
 * <p/>
 * Clusters have the center of the first element (not the centroid of the items within it).
 */
open class NonHierarchicalDistanceBasedAlgorithm<T : TedClusterItem> : Algorithm<T> {

    override var maxDistanceBetweenClusteredItems = DEFAULT_MAX_DISTANCE_AT_ZOOM

    /**
     * Any modifications should be synchronized on mQuadTree.
     */
    private val mItems = HashSet<QuadItem<T>>()

    /**
     * Any modifications should be synchronized on mQuadTree.
     */
    private val mQuadTree = PointQuadTree<QuadItem<T>>(0.0, 1.0, 0.0, 1.0)

    override val items: Collection<T>
        get() {
            val items = ArrayList<T>()
            synchronized(mQuadTree) {
                for (quadItem in mItems) {
                    items.add(quadItem.mClusterItem)
                }
            }
            return items
        }

    override fun addItem(item: T) {
        val quadItem = QuadItem(item)
        synchronized(mQuadTree) {
            mItems.add(quadItem)
            mQuadTree.add(quadItem)
        }
    }

    override fun addItems(items: Collection<T>) {
        for (item in items) {
            addItem(item)
        }
    }

    override fun clearItems() {
        synchronized(mQuadTree) {
            mItems.clear()
            mQuadTree.clear()
        }
    }

    override fun removeItem(item: T) {
        // QuadItem delegates hashcode() and equals() to its item so,
        //   removing any QuadItem to that item will remove the item
        val quadItem = QuadItem(item)
        synchronized(mQuadTree) {
            mItems.remove(quadItem)
            mQuadTree.remove(quadItem)
        }
    }

    override fun getClusters(zoom: Double): Set<Cluster<T>> {
        val discreteZoom = zoom.toInt()

        val zoomSpecificSpan = maxDistanceBetweenClusteredItems.toDouble() / Math.pow(
            2.0,
            discreteZoom.toDouble()
        ) / 256.0

        val visitedCandidates = HashSet<QuadItem<T>>()
        val results = HashSet<Cluster<T>>()
        val distanceToCluster = HashMap<QuadItem<T>, Double>()
        val itemToCluster = HashMap<QuadItem<T>, StaticCluster<T>>()

        synchronized(mQuadTree) {
            for (candidate in getClusteringItems(mQuadTree, discreteZoom)) {
                if (visitedCandidates.contains(candidate)) {
                    // Candidate is already part of another cluster.
                    continue
                }

                val searchBounds = createBoundsFromSpan(candidate.point, zoomSpecificSpan)
                val clusterItems: Collection<QuadItem<T>>
                clusterItems = mQuadTree.search(searchBounds)
                if (clusterItems.size == 1) {
                    // Only the current marker is in range. Just add the single item to the results.
                    results.add(candidate)
                    visitedCandidates.add(candidate)
                    distanceToCluster[candidate] = 0.0
                    continue
                }
                val cluster = StaticCluster<T>(candidate.mClusterItem.getTedLatLng())
                results.add(cluster)

                for (clusterItem in clusterItems) {
                    val existingDistance = distanceToCluster[clusterItem]
                    val distance = distanceSquared(clusterItem.point, candidate.point)
                    if (existingDistance != null) {
                        // Item already belongs to another cluster. Check if it's closer to this cluster.
                        if (existingDistance < distance) {
                            continue
                        }
                        // Move item to the closer cluster.
                        itemToCluster[clusterItem]!!.remove(clusterItem.mClusterItem)
                    }
                    distanceToCluster[clusterItem] = distance
                    cluster.add(clusterItem.mClusterItem)
                    itemToCluster[clusterItem] = cluster
                }
                visitedCandidates.addAll(clusterItems)
            }
        }
        return results
    }

    internal open fun getClusteringItems(
        quadTree: PointQuadTree<QuadItem<T>>,
        discreteZoom: Int
    ): Collection<QuadItem<T>> {
        return mItems
    }

    private fun distanceSquared(a: Point, b: Point): Double {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
    }

    private fun createBoundsFromSpan(p: Point, span: Double): Bounds {
        // TODO: Use a span that takes into account the visual size of the marker, not just its
        // LatLng.
        val halfSpan = span / 2
        return Bounds(
            p.x - halfSpan, p.x + halfSpan,
            p.y - halfSpan, p.y + halfSpan
        )
    }

    internal class QuadItem<T : TedClusterItem>(val mClusterItem: T) : PointQuadTree.Item, Cluster<T> {
        override val point: Point
        override val position: TedLatLng = mClusterItem.getTedLatLng()
        override val items: Set<T>

        override val size: Int
            get() = 1

        init {
            point = PROJECTION.toPoint(position)
            items = setOf(mClusterItem)
        }

        override fun hashCode(): Int {
            return mClusterItem.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return (other as? QuadItem<*>)?.mClusterItem?.equals(mClusterItem) ?: false

        }
    }

    companion object {
        private const val DEFAULT_MAX_DISTANCE_AT_ZOOM = 100 // essentially 100 dp.

        private val PROJECTION = SphericalMercatorProjection(1.0)
    }
}
