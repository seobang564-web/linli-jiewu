package com.linlijiewu.app.ui.publish;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.ActivityPickupBinding;
import com.linlijiewu.app.databinding.ItemPoiBinding;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.util.AmapPrivacy;

/**
 * 选择交接点：地图中心固定大头针，拖动地图选点，下方为附近地点。
 * 结果返回 pickupLat / pickupLng（GCJ-02）与 pickupLabel；POI 列表目前为示例，后续接高德 POI 搜索。
 */
public class PickupActivity extends AppCompatActivity {
    public static final String EXTRA_LABEL = "pickup_label";
    public static final String EXTRA_LAT = "pickup_lat";
    public static final String EXTRA_LNG = "pickup_lng";

    private ActivityPickupBinding binding;
    private MapView mapView;
    private AMap map;
    private LatLng target = new LatLng(24.82615, 102.85477);
    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPickupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.top.topBarRoot);
        Ui.padBottom(binding.bottomBar);
        Ui.back(this, binding.top.back);
        binding.top.title.setText(R.string.pickup_title);

        renderPois();
        updateCoord();
        binding.labelInput.setText(SampleData.PICKUP_POIS[0][0]);
        binding.locate.setOnClickListener(v -> {
            Motion.bounce(v);
            if (map != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(home(), 17f), 500, null);
            }
        });
        binding.confirm.setOnClickListener(v -> {
            String label = binding.labelInput.getText().toString().trim();
            setResult(RESULT_OK, new Intent()
                    .putExtra(EXTRA_LABEL, label.isEmpty() ? SampleData.PICKUP_POIS[selected][0] : label)
                    .putExtra(EXTRA_LAT, target.latitude)
                    .putExtra(EXTRA_LNG, target.longitude));
            finish();
        });

        if (AmapPrivacy.isAgreed(this)) {
            initMap(savedInstanceState);
        }
        Motion.stagger(binding.poiList, 160);
    }

    private LatLng home() {
        if (BuildConfig.DEBUG && !BuildConfig.MOCK_LOCATION.isEmpty()) {
            String[] p = BuildConfig.MOCK_LOCATION.split(",");
            return new LatLng(Double.parseDouble(p[0]), Double.parseDouble(p[1]));
        }
        return target;
    }

    private void initMap(Bundle state) {
        AmapPrivacy.syncToSdk(this);
        mapView = new MapView(this);
        binding.mapContainer.addView(mapView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mapView.onCreate(state);
        binding.mapFallback.setVisibility(View.GONE);
        map = mapView.getMap();
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        target = home();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 17f));
        if (BuildConfig.MOCK_LOCATION.isEmpty() && hasPermission()) {
            map.setMyLocationStyle(new MyLocationStyle().myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE));
            map.setMyLocationEnabled(true);
        }
        map.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                // 拖动时大头针抬起
                binding.centerPin.setTranslationY(-Ui.dp(PickupActivity.this, 10));
            }

            @Override
            public void onCameraChangeFinish(CameraPosition position) {
                target = position.target;
                updateCoord();
                binding.centerPin.animate().translationY(0).setDuration(380)
                        .setInterpolator(Motion.spring(PickupActivity.this)).start();
            }
        });
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateCoord() {
        binding.coord.setText(getString(R.string.pickup_coord, target.latitude, target.longitude));
    }

    private void renderPois() {
        // 地点卡片插在“交接点名称”卡片之前
        while (binding.poiList.getChildCount() > 1) {
            binding.poiList.removeViewAt(0);
        }
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < SampleData.PICKUP_POIS.length; i++) {
            String[] poi = SampleData.PICKUP_POIS[i];
            ItemPoiBinding row = ItemPoiBinding.inflate(inflater, binding.poiList, false);
            row.name.setText(poi[0]);
            row.address.setText(poi[1]);
            row.distance.setText(getString(R.string.pickup_distance, Integer.parseInt(poi[2])));
            boolean on = i == selected;
            row.getRoot().setSelected(on);
            row.check.setVisibility(on ? View.VISIBLE : View.GONE);
            final int index = i;
            row.getRoot().setOnClickListener(v -> {
                selected = index;
                binding.labelInput.setText(poi[0]);
                renderPois();
                Motion.popIn(((ViewGroup) binding.poiList.getChildAt(index)).getChildAt(1), 0);
            });
            binding.poiList.addView(row.getRoot(), i);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            map.setMyLocationEnabled(false);
            mapView.onDestroy();
        }
        super.onDestroy();
    }
}
