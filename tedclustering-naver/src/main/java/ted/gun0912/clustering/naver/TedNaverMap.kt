package ted.gun0912.clustering.naver

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import ted.gun0912.clustering.TedMap
import ted.gun0912.clustering.geometry.TedCameraPosition
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.geometry.TedLatLngBounds

class TedNaverMap(private val naverMap: NaverMap) : TedMap<Marker, TedNaverMarker, OverlayImage> {

    override fun getCameraPosition(): TedCameraPosition {
        val cameraPosition = naverMap.cameraPosition
        val tedLatLng = TedLatLng(cameraPosition.target.latitude, cameraPosition.target.longitude)
        return TedCameraPosition(
            tedLatLng,
            cameraPosition.zoom,
            cameraPosition.tilt,
            cameraPosition.bearing
        )
    }

    override fun addOnCameraIdleListener(onCameraIdleListener: (tedCameraPosition: TedCameraPosition) -> Unit) {
        /*
        naverMap.addOnCameraChangeListener({ reason, animated ->
            Log.d("ted", "카메라 변경 - reson: $reason, animated: $animated")
            onCameraIdleListener.invoke(getCameraPosition())
        })
        */
        naverMap.addOnCameraIdleListener { onCameraIdleListener.invoke(getCameraPosition()) }
    }

    override fun addMarker(markerTed: TedNaverMarker) {
        markerTed.marker.map = naverMap
    }

    override fun removeMarker(markerTed: TedNaverMarker) {
        markerTed.marker.map = null
    }

    override fun getVisibleLatLngBounds(): TedLatLngBounds =
        TedLatLngBounds().apply {
            val bounds = naverMap.contentBounds

            southWest = TedLatLng(
                bounds.southWest.latitude,
                bounds.southWest.longitude
            )
            northEast = TedLatLng(
                bounds.northEast.latitude,
                bounds.northEast.longitude
            )
        }


    override fun moveToCenter(tedLatLng: TedLatLng) {
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(tedLatLng.latitude, tedLatLng.longitude))
            //.animate(CameraAnimation.Easing)
            .animate(CameraAnimation.Linear)
        naverMap.moveCamera(cameraUpdate)
    }


    override fun getMarker(): TedNaverMarker {
        return getMarker(Marker())
    }

    override fun getMarker(marker: Marker): TedNaverMarker {
        return TedNaverMarker(marker)
    }

    override fun addMarkerClickListener(
        tedNaverMarker: TedNaverMarker,
        action: (TedNaverMarker) -> Unit
    ) {
        tedNaverMarker.marker.setOnClickListener {
            action.invoke(tedNaverMarker)
            true
        }
    }
}