package ted.gun0912.clustering.naver.demo;

import androidx.annotation.NonNull;

import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.NaverMap;

import java.util.ArrayList;
import java.util.List;

import ted.gun0912.clustering.naver.TedNaverClustering;

public class JavaActivity extends BaseDemoActivity {
    private NaverMap naverMap;

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        naverMap.moveCamera(
                CameraUpdate.toCameraPosition(
                        new CameraPosition(NaverMap.DEFAULT_CAMERA_POSITION.target, NaverMap.DEFAULT_CAMERA_POSITION.zoom))
        );

        TedNaverClustering.with(this, naverMap)
                .items(getItems())
                .make();

    }


    private List<NaverItem> getItems() {
        LatLngBounds bounds = naverMap.getContentBounds();
        ArrayList<NaverItem> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            NaverItem temp = new NaverItem((bounds.getNorthLatitude() - bounds.getSouthLatitude()) * Math.random() + bounds.getSouthLatitude(),
                    (bounds.getEastLongitude() - bounds.getWestLongitude()) * Math.random() + bounds.getWestLongitude()
            );
            items.add(temp);
        }
        return items;

    }
}
