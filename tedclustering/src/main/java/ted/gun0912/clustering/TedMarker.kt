package ted.gun0912.clustering

import android.graphics.Bitmap
import ted.gun0912.clustering.geometry.TedLatLng


interface TedMarker<ImageDescriptor> {

    fun setVisible(visible: Boolean)

    var position: TedLatLng

    fun setImageDescriptor(imageDescriptor: ImageDescriptor)

    fun fromBitmap(bitmap: Bitmap): ImageDescriptor
}