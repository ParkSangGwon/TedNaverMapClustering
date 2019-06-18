package ted.gun0912.clustering

import ted.gun0912.clustering.geometry.TedCameraPosition
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.geometry.TedLatLngBounds


interface TedMap<RealMarker, TM : TedMarker<ImageDescriptor>, ImageDescriptor> {

    fun getCameraPosition(): TedCameraPosition

    fun addOnCameraIdleListener(onCameraIdleListener: ((tedCameraPosition: TedCameraPosition) -> Unit))

    fun addMarker(marker: TM)

    fun removeMarker(marker: TM)

    fun getVisibleLatLngBounds(): TedLatLngBounds

    fun moveToCenter(tedLatLng: TedLatLng)

    fun getMarker(): TM

    fun getMarker(marker: RealMarker): TM

    fun addMarkerClickListener(marker: TM, action: ((TM) -> Unit))
}