package ted.gun0912.clustering.naver.demo;

import com.naver.maps.geometry.LatLng;

import org.jetbrains.annotations.NotNull;

import ted.gun0912.clustering.clustering.TedClusterItem;
import ted.gun0912.clustering.geometry.TedLatLng;

public class JavaItem implements TedClusterItem {

    private LatLng latLng;

    public JavaItem(LatLng latLng) {
        this.latLng = latLng;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    @NotNull
    @Override
    public TedLatLng getTedLatLng() {
        return new TedLatLng(latLng.latitude, latLng.longitude);
    }
}
