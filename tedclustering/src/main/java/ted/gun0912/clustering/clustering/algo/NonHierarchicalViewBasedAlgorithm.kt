package ted.gun0912.clustering.clustering.algo

import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.geometry.Bounds
import ted.gun0912.clustering.geometry.TedCameraPosition
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.projection.SphericalMercatorProjection
import ted.gun0912.clustering.quadtree.PointQuadTree


class NonHierarchicalViewBasedAlgorithm<T : TedClusterItem>(
    private var mViewWidth: Int,
    private var mViewHeight: Int
) : NonHierarchicalDistanceBasedAlgorithm<T>(), ScreenBasedAlgorithm<T> {

    private var mMapCenter: TedLatLng? = null

    override fun onCameraChange(tedCameraPosition: TedCameraPosition) {
        mMapCenter = tedCameraPosition.target
    }

    override fun getClusteringItems(
        quadTree: PointQuadTree<QuadItem<T>>,
        discreteZoom: Int
    ): Collection<QuadItem<T>> {
        return quadTree.search(getVisibleBounds(discreteZoom))
    }

    override fun shouldReClusterOnMapMovement(): Boolean {
        return true
    }

    /**
     * Update view width and height in case map size was changed.
     * You need to recluster all the clusters, to update view state after view size changes.
     * @param width map width
     * @param height map height
     */
    fun updateViewSize(width: Int, height: Int) {
        mViewWidth = width
        mViewHeight = height
    }

    private fun getVisibleBounds(zoom: Int): Bounds {
        if (mMapCenter == null) {
            return Bounds(0.0, 0.0, 0.0, 0.0)
        }

        val p = PROJECTION.toPoint(mMapCenter!!)

        val halfWidthSpan = mViewWidth.toDouble() / Math.pow(2.0, zoom.toDouble()) / 256.0 / 2.0
        val halfHeightSpan = mViewHeight.toDouble() / Math.pow(2.0, zoom.toDouble()) / 256.0 / 2.0

        return Bounds(
            p.x - halfWidthSpan, p.x + halfWidthSpan,
            p.y - halfHeightSpan, p.y + halfHeightSpan
        )
    }

    companion object {

        private val PROJECTION = SphericalMercatorProjection(1.0)
    }
}
