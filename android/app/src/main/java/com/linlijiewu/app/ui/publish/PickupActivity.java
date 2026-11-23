package com.linlijiewu.app.ui.publish;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.ActivityPickupBinding;
import com.linlijiewu.app.databinding.ItemPoiBinding;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.SearchLocation;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.util.AmapPrivacy;
import com.linlijiewu.app.util.PlaceSearch;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图选点：地图中心固定大头针，拖动地图选点；下方是高德逆地理返回的周边地点，
 * 顶部可按关键词搜索地点。结果返回 pickupLat / pickupLng（GCJ-02）与 pickupLabel。
 * 发布页用它选交接点；发现页用 {@link #MODE_SEARCH_CENTER} 模式选“在哪附近找”。
 */
public class PickupActivity extends AppCompatActivity {
    public static final String EXTRA_LABEL = "pickup_label";
    public static final String EXTRA_LAT = "pickup_lat";
    public static final String EXTRA_LNG = "pickup_lng";
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_SEARCH_CENTER = "search_center";

    private static final int NEARBY_RADIUS_M = 300;
    private static final long REGEO_DELAY_MS = 350;
    private static final long SEARCH_DELAY_MS = 400;
    /** 没有任何位置信息时的初始视角：云南大学呈贡校区。 */
    private static final LatLng FALLBACK = new LatLng(24.82615, 102.85477);

    private ActivityPickupBinding binding;
    private MapView mapView;
    private AMap map;
    private LatLng target;
    private PlaceSearch places;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PlaceSearch.Place> shown = new ArrayList<>();
    private int selected = -1;
    private boolean searching;
    private boolean labelEdited;
    private boolean settingLabel;
    /** 点选地点后由程序移动地图，这次移动结束时不刷新周边列表，避免选中项被冲掉。 */
    private boolean keepListOnNextMove;
    private boolean centeredOnLocation;
    @Nullable private String city;
    @Nullable private String address;
    private boolean searchCenterMode;

    private final Runnable regeo = this::loadNearby;
    private final Runnable search = this::runSearch;

    /** 发布页：以已选交接点为初始位置（可为空）。 */
    public static Intent pickup(Context c, @Nullable Double lat, @Nullable Double lng, @Nullable String label) {
        Intent i = new Intent(c, PickupActivity.class);
        if (lat != null && lng != null) {
            i.putExtra(EXTRA_LAT, lat).putExtra(EXTRA_LNG, lng).putExtra(EXTRA_LABEL, label);
        }
        return i;
    }

    /** 发现页：选择检索中心。 */
    public static Intent searchCenter(Context c, @Nullable LatLng current) {
        Intent i = new Intent(c, PickupActivity.class).putExtra(EXTRA_MODE, MODE_SEARCH_CENTER);
        if (current != null) {
            i.putExtra(EXTRA_LAT, current.latitude).putExtra(EXTRA_LNG, current.longitude);
        }
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPickupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.top.topBarRoot);
        Ui.padBottom(binding.bottomBar);
        Ui.back(this, binding.top.back);
        places = new PlaceSearch(this);

        Intent in = getIntent();
        searchCenterMode = MODE_SEARCH_CENTER.equals(in.getStringExtra(EXTRA_MODE));
        binding.top.title.setText(searchCenterMode ? R.string.pickup_title_center : R.string.pickup_title);
        binding.confirm.setText(searchCenterMode ? R.string.pickup_confirm_center : R.string.pickup_confirm);
        binding.labelCard.setVisibility(searchCenterMode ? View.GONE : View.VISIBLE);
        if (searchCenterMode) {
            binding.dragHint.setText(R.string.pickup_drag_hint_center);
        }
        binding.city.setText(R.string.pickup_city_locating);

        boolean hasInitial = in.hasExtra(EXTRA_LAT) && in.hasExtra(EXTRA_LNG);
        if (hasInitial) {
            target = new LatLng(in.getDoubleExtra(EXTRA_LAT, 0), in.getDoubleExtra(EXTRA_LNG, 0));
            centeredOnLocation = true; // 已有位置，不再被定位结果拉走
            String label = in.getStringExtra(EXTRA_LABEL);
            if (label != null && !label.isEmpty()) {
                setLabel(label);
                labelEdited = true; // 保留原名称，除非用户点选其它地点
            }
        } else {
            target = home();
        }
        updateCoord();

        binding.labelInput.addTextChangedListener(new SimpleWatcher(() -> {
            if (!settingLabel) {
                labelEdited = true;
            }
        }));
        binding.searchInput.addTextChangedListener(new SimpleWatcher(() -> {
            handler.removeCallbacks(search);
            boolean empty = keyword().isEmpty();
            binding.clearSearch.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                if (searching) {
                    searching = false;
                    loadNearby();
                }
            } else {
                handler.postDelayed(search, SEARCH_DELAY_MS);
            }
        }));
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(search);
                runSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });
        binding.clearSearch.setOnClickListener(v -> binding.searchInput.setText(""));
        binding.locate.setOnClickListener(v -> {
            Motion.bounce(v);
            LatLng here = currentLocation();
            if (map != null && here != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 17f), 500, null);
            } else {
                Toast.makeText(this, R.string.pickup_no_location, Toast.LENGTH_SHORT).show();
            }
        });
        binding.confirm.setOnClickListener(v -> confirm());

        if (AmapPrivacy.isAgreed(this)) {
            initMap(savedInstanceState);
            loadNearby();
        } else {
            showMessage(getString(R.string.pickup_privacy_needed));
        }
    }

    private void confirm() {
        String label = binding.labelInput.getText().toString().trim();
        if (label.isEmpty() && selected >= 0 && selected < shown.size()) {
            label = shown.get(selected).name;
        }
        if (searchCenterMode) {
            if (label.isEmpty()) {
                label = address != null && !address.isEmpty() ? address : getString(R.string.pickup_center_default);
            }
        } else if (label.isEmpty()) {
            binding.labelInput.setError(getString(R.string.pickup_label_needed));
            binding.labelInput.requestFocus();
            return;
        }
        setResult(RESULT_OK, new Intent()
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_LAT, target.latitude)
                .putExtra(EXTRA_LNG, target.longitude));
        finish();
    }

    /** 调试固定坐标 → 发现页最近一次定位 → 校区默认点。 */
    private LatLng home() {
        if (BuildConfig.DEBUG && !BuildConfig.MOCK_LOCATION.isEmpty()) {
            String[] p = BuildConfig.MOCK_LOCATION.split(",");
            return new LatLng(Double.parseDouble(p[0]), Double.parseDouble(p[1]));
        }
        if (SearchLocation.available(this)) {
            return new LatLng(SearchLocation.lat(this), SearchLocation.lng(this));
        }
        return FALLBACK;
    }

    @Nullable
    private LatLng currentLocation() {
        if (BuildConfig.DEBUG && !BuildConfig.MOCK_LOCATION.isEmpty()) {
            return home();
        }
        if (map != null && map.getMyLocation() != null
                && (map.getMyLocation().getLatitude() != 0 || map.getMyLocation().getLongitude() != 0)) {
            return new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude());
        }
        return SearchLocation.available(this) ? new LatLng(SearchLocation.lat(this), SearchLocation.lng(this)) : null;
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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 17f));
        if (BuildConfig.MOCK_LOCATION.isEmpty() && hasPermission()) {
            // 只显示蓝点，不自动移动视角；首次定位成功且没有初始位置时才移过去
            map.setMyLocationStyle(new MyLocationStyle()
                    .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER).interval(5000));
            map.setMyLocationEnabled(true);
            map.setOnMyLocationChangeListener(location -> {
                if (centeredOnLocation || location == null
                        || (location.getLatitude() == 0 && location.getLongitude() == 0)) {
                    return;
                }
                centeredOnLocation = true;
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), 17f), 500, null);
            });
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
                if (keepListOnNextMove) {
                    keepListOnNextMove = false;
                    return;
                }
                if (!searching) {
                    handler.removeCallbacks(regeo);
                    handler.postDelayed(regeo, REGEO_DELAY_MS);
                }
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

    // ---------------------------------------------------------------- 周边地点与搜索

    private void loadNearby() {
        if (binding == null || !AmapPrivacy.isAgreed(this)) {
            return;
        }
        LatLng at = target;
        showMessage(getString(R.string.pickup_nearby_loading));
        places.reverse(at, NEARBY_RADIUS_M, (result, code) -> {
            if (binding == null || searching) {
                return;
            }
            if (result == null) {
                showMessage(getString(R.string.pickup_nearby_failed, code));
                return;
            }
            city = result.city;
            address = result.formatted;
            binding.city.setText(city == null || city.isEmpty() ? getString(R.string.pickup_city) : city);
            render(result.nearby, labelEdited ? -1 : 0);
            if (result.nearby.isEmpty()) {
                showMessage(getString(R.string.pickup_nearby_empty));
            }
        });
    }

    private void runSearch() {
        String keyword = keyword();
        if (binding == null || keyword.isEmpty() || !AmapPrivacy.isAgreed(this)) {
            return;
        }
        searching = true;
        handler.removeCallbacks(regeo);
        showMessage(getString(R.string.pickup_search_loading));
        places.keyword(keyword, city, target, (result, code) -> {
            if (binding == null || !searching || !keyword.equals(keyword())) {
                return;
            }
            if (result == null) {
                showMessage(getString(R.string.pickup_search_failed, code));
            } else if (result.isEmpty()) {
                showMessage(getString(R.string.pickup_search_empty, keyword));
            } else {
                render(result, -1);
            }
        });
    }

    private String keyword() {
        return binding.searchInput.getText().toString().trim();
    }

    /** 地点卡片插在“交接点名称”卡片之前。 */
    private void render(List<PlaceSearch.Place> list, int select) {
        if (list != shown) {
            shown.clear();
            shown.addAll(list);
        }
        selected = select;
        if (select >= 0 && select < shown.size()) {
            setLabel(shown.get(select).name);
        }
        clearList();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < shown.size(); i++) {
            PlaceSearch.Place place = shown.get(i);
            ItemPoiBinding row = ItemPoiBinding.inflate(inflater, binding.poiList, false);
            row.name.setText(place.name);
            row.address.setText(place.address == null || place.address.isEmpty() ? getString(R.string.pickup_no_address) : place.address);
            row.distance.setText(Ui.distanceText(this, place.distanceM));
            boolean on = i == selected;
            row.getRoot().setSelected(on);
            row.check.setVisibility(on ? View.VISIBLE : View.GONE);
            final int index = i;
            row.getRoot().setOnClickListener(v -> choose(index));
            binding.poiList.addView(row.getRoot(), i);
        }
    }

    private void choose(int index) {
        PlaceSearch.Place place = shown.get(index);
        labelEdited = false;
        hideKeyboard();
        render(shown, index);
        Motion.popIn(((ViewGroup) binding.poiList.getChildAt(index)).getChildAt(1), 0);
        LatLng to = new LatLng(place.lat, place.lng);
        target = to;
        updateCoord();
        // 位置没变时地图不会回调移动结束，标记会残留到下一次拖动，所以只在确实要移动时设置
        if (map != null && AMapUtils.calculateLineDistance(map.getCameraPosition().target, to) > 1f) {
            keepListOnNextMove = true;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(to, Math.max(17f, map.getCameraPosition().zoom)), 400, null);
        }
    }

    private void setLabel(String label) {
        settingLabel = true;
        binding.labelInput.setText(label);
        binding.labelInput.setError(null);
        settingLabel = false;
    }

    private void showMessage(String message) {
        clearList();
        TextView text = new TextView(this, null, 0, R.style.T_Sub);
        text.setText(message);
        int pad = Ui.dp(this, 20);
        text.setPadding(pad, Ui.dp(this, 8), pad, Ui.dp(this, 18));
        binding.poiList.addView(text, 0);
    }

    private void clearList() {
        while (binding.poiList.getChildCount() > 1) {
            binding.poiList.removeViewAt(0);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
        }
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable after;

        SimpleWatcher(Runnable after) {
            this.after = after;
        }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void afterTextChanged(Editable s) { after.run(); }
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
        handler.removeCallbacksAndMessages(null);
        places.cancel();
        if (mapView != null) {
            map.setMyLocationEnabled(false);
            mapView.onDestroy();
        }
        super.onDestroy();
    }
}
