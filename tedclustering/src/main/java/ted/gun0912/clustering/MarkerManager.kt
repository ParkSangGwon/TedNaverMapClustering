package ted.gun0912.clustering

import java.util.*

/**
 * Keeps track of collections of markers on the tedMap. Delegates all TedMarker-related events to each
 * collection's individually managed listeners.
 *
 *
 * All marker operations (adds and removes) should occur via its collection class. That is, don't
 * add a marker via a collection, then remove it via TedMarker.remove()
 */
class MarkerManager<RealMarker,Marker : TedMarker<ImageDescriptor>, ImageDescriptor>(
    private val tedMap: TedMap<RealMarker,Marker, ImageDescriptor>
) {
    private val allMarkerMap = HashMap<Marker, MarkerCollection>()

    fun newCollection(): MarkerCollection = MarkerCollection()

    fun remove(marker: Marker) {
        allMarkerMap[marker]?.remove(marker)
    }

    fun onMarkerClick(marker: Marker) {
        val markerCollection = allMarkerMap[marker]
        markerCollection?.markerClickListener?.invoke(marker)
    }

    inner class MarkerCollection {
        private val mMarkers = HashSet<Marker>()
        var markerClickListener: ((Marker) -> Unit)? = null

        fun addMarker(marker: Marker): Marker {
            tedMap.addMarker(marker)
            mMarkers.add(marker)
            allMarkerMap[marker] = this@MarkerCollection
            return marker
        }

        fun addAll(collection: Collection<Marker>) {
            for (marker in collection) {
                addMarker(marker)
            }
        }

        fun addAll(collection: Collection<Marker>, defaultVisible: Boolean) {
            for (marker in collection) {
                addMarker(marker).setVisible(defaultVisible)
            }
        }

        fun showAll() {
            for (marker in mMarkers) {
                marker.setVisible(true)
            }
        }

        fun hideAll() {
            for (marker in mMarkers) {
                marker.setVisible(false)
            }
        }

        fun remove(tedMarker: Marker): Boolean {
            if (mMarkers.remove(tedMarker)) {
                allMarkerMap.remove(tedMarker)
                tedMap.removeMarker(tedMarker)
                return true
            }
            return false
        }

        fun clear() {
            for (marker in mMarkers) {
                tedMap.removeMarker(marker)
                allMarkerMap.remove(marker)
            }
            mMarkers.clear()
        }


        fun setOnMarkerClickListener(markerClickListener: ((Marker) -> Unit)) {
            this.markerClickListener = markerClickListener
        }


    }
}
