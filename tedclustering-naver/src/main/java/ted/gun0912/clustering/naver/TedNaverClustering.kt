package ted.gun0912.clustering.naver

import android.content.Context
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import ted.gun0912.clustering.BaseBuilder
import ted.gun0912.clustering.TedClustering
import ted.gun0912.clustering.clustering.ClusterManager
import ted.gun0912.clustering.clustering.TedClusterItem

class TedNaverClustering<C : TedClusterItem>(clusterManager: ClusterManager<TedNaverClustering<C>, C, Marker, TedNaverMarker, NaverMap, OverlayImage>) :
    TedClustering<TedNaverClustering<C>, C, Marker, TedNaverMarker, NaverMap, OverlayImage>(
        clusterManager
    ) {

    companion object {
        @JvmStatic
        fun <C : TedClusterItem> with(context: Context, map: NaverMap) =
            Builder<C>(context, map)
    }

    class Builder<C : TedClusterItem>(context: Context, map: NaverMap) :
        BaseBuilder<TedNaverClustering<C>, C, Marker, TedNaverMarker, NaverMap, OverlayImage>(
            context,
            TedNaverMap(map)
        ) {
        override fun make(): TedNaverClustering<C> {
            return TedNaverClustering(ClusterManager(this))
        }
    }

}