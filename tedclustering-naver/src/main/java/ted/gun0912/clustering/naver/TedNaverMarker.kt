package ted.gun0912.clustering.naver

import android.graphics.Bitmap
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import ted.gun0912.clustering.TedMarker
import ted.gun0912.clustering.geometry.TedLatLng

class TedNaverMarker(val marker: Marker) : TedMarker<OverlayImage> {

    override fun setVisible(visible: Boolean) {
        marker.isVisible = visible
    }

    override var position: TedLatLng
        get() = TedLatLng(marker.position.latitude, marker.position.longitude)
        set(value) {
            marker.position = LatLng(value.latitude, value.longitude)
        }

    override fun setImageDescriptor(imageDescriptor: OverlayImage) {
        marker.icon = imageDescriptor
    }

    override fun fromBitmap(bitmap: Bitmap): OverlayImage = OverlayImage.fromBitmap(bitmap)

}