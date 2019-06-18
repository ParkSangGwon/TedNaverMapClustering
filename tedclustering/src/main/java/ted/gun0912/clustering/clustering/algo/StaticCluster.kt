package ted.gun0912.clustering.clustering.algo

import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.geometry.TedLatLng


/**
 * A cluster whose center is determined upon creation.
 */
class StaticCluster<T : TedClusterItem>(override val position: TedLatLng) : Cluster<T> {
    private val mItems = ArrayList<T>()

    override val items: Collection<T>
        get() = mItems

    override val size: Int
        get() = mItems.size

    fun add(t: T): Boolean {
        return mItems.add(t)
    }

    fun remove(t: T): Boolean {
        return mItems.remove(t)
    }

    override fun toString(): String {
        return "StaticCluster{" +
                "mCenter=" + position +
                ", mItems.size=" + mItems.size +
                '}'.toString()
    }

    override fun hashCode(): Int {
        return position.hashCode() + mItems.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is StaticCluster<*>) {
            false
        } else other.position == position && other.mItems == mItems

    }
}