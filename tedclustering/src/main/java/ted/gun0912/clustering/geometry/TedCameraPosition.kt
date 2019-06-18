package ted.gun0912.clustering.geometry

data class TedCameraPosition(
    var target: TedLatLng,
    var zoom: Double,
    var tilt: Double?,
    var bearing: Double?
)