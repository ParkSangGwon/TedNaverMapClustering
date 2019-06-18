package ted.gun0912.clustering.geometry

data class TedLatLngBounds(
    var southWest: TedLatLng = TedLatLng(0.0, 0.0),
    var northEast: TedLatLng = TedLatLng(0.0, 0.0)
) {

    constructor(
        northLatitude: Double,
        eastLongitude: Double,
        southLatitude: Double,
        westLongitude: Double
    ) : this(TedLatLng(southLatitude, westLongitude), TedLatLng(northLatitude, eastLongitude))

    operator fun contains(tedLatLng: TedLatLng): Boolean =
        containLatLng(tedLatLng.latitude, southWest.latitude, northEast.latitude)
                && containLatLng(tedLatLng.longitude, southWest.longitude, northEast.longitude)

    private fun containLatLng(target: Double, value1: Double, value2: Double) =
        if (value1 < value2) {
            target in value1..value2
        } else {
            target in value2..value1
        }
}