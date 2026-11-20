package com.linlijiewu.app.ui.discover;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.model.ItemStatus;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.FragmentDiscoverBinding;
import com.linlijiewu.app.databinding.ItemCategoryBinding;
import com.linlijiewu.app.databinding.ItemNearbyBinding;
import com.linlijiewu.app.databinding.ViewMapPinBinding;
import com.linlijiewu.app.ui.browse.BrowseActivity;
import com.linlijiewu.app.ui.common.LoadingDialog;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.item.ItemDetailActivity;
import com.linlijiewu.app.ui.publish.PublishActivity;
import com.linlijiewu.app.util.AmapPrivacy;
import java.util.ArrayList;
import java.util.List;

/**
 * 发现页：高德地图 + 当前位置 + 附近物品。
 * 地图流程不变：隐私合规弹窗 → 创建 MapView → 申请定位权限 → 持续定位，首次成功时把视角移到当前位置。
 * 定位成功后在周围摆放物品标记与搜索半径圈；物品目前来自 {@link SampleData}，GET /items 接入后替换。
 */
public class DiscoverFragment extends Fragment {
    private static final String KEY_MAP_STATE = "map_state";
    private static final float DEFAULT_ZOOM = 15.5f;
    private static final long LOCATION_INTERVAL_MS = 5000;
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int[] RADII = {500, 1000, 3000, 5000};

    private enum State { PRIVACY_REQUIRED, PERMISSION_DENIED, LOCATING, LOCATED, FAILED }

    private FragmentDiscoverBinding binding;
    private MapView mapView;
    private AMap map;
    private State state;
    private boolean permissionPermanentlyDenied;
    private boolean hasCentered;
    private LatLng center;
    private int radius = 1000;
    private Circle radiusCircle;
    private final List<Marker> markers = new ArrayList<>();
    private boolean entered;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (binding == null) {
                    return;
                }
                if (hasLocationPermission()) {
                    startLocating();
                } else {
                    permissionPermanentlyDenied =
                            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                    showState(State.PERMISSION_DENIED, null);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Ui.padTop(binding.topOverlay);
        ViewCompat.setOnApplyWindowInsetsListener(binding.sheet, (sheet, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            sheet.setPadding(0, 0, 0, Ui.dp(requireContext(), 110) + bars.bottom);
            return insets;
        });
        setupSheet();
        setupRadius();
        renderCategories();
        renderList();

        binding.statusAction.setOnClickListener(v -> onStatusAction());
        binding.locateButton.setOnClickListener(v -> recenter());
        binding.searchBar.setOnClickListener(v -> startActivity(new Intent(requireContext(), BrowseActivity.class)));
        binding.goBorrow.setOnClickListener(v -> startActivity(new Intent(requireContext(), BrowseActivity.class)));
        binding.goLend.setOnClickListener(v -> PublishActivity.open(requireActivity()));
        binding.switchPlace.setOnClickListener(v -> Ui.comingSoon(requireContext()));

        if (AmapPrivacy.isAgreed(requireContext())) {
            initMap(savedInstanceState == null ? null : savedInstanceState.getBundle(KEY_MAP_STATE));
        } else {
            showState(State.PRIVACY_REQUIRED, null);
            showPrivacyDialog();
        }
    }

    private void setupSheet() {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(binding.sheet);
        binding.getRoot().post(() -> {
            if (binding != null) {
                behavior.setPeekHeight(Math.round(binding.getRoot().getHeight() * 0.5f));
            }
        });
        if (!entered && Motion.enabled(requireContext())) {
            entered = true;
            binding.sheet.setTranslationY(Ui.dp(requireContext(), 80));
            binding.sheet.setAlpha(0f);
            binding.sheet.animate().translationY(0f).alpha(1f).setStartDelay(120).setDuration(620)
                    .setInterpolator(Motion.easeOut(requireContext())).start();
            Motion.stagger(binding.sheetContent, 220);
        }
    }

    private void setupRadius() {
        TextView[] chips = {binding.radius500, binding.radius1000, binding.radius3000, binding.radius5000};
        for (int i = 0; i < chips.length; i++) {
            final int r = RADII[i];
            chips[i].setSelected(r == radius);
            chips[i].setOnClickListener(v -> {
                if (r == radius) {
                    return;
                }
                for (TextView c : chips) {
                    c.setSelected(c == v);
                }
                Motion.bounce(v);
                // 原型阶段模拟一次检索请求；GET /items 接入后在回调里刷新
                LoadingDialog.run(requireContext(), R.string.loading_search, () -> applyRadius(r));
            });
        }
    }

    private void applyRadius(int newRadius) {
        if (binding == null) {
            return;
        }
        int old = radius;
        radius = newRadius;
        renderList();
        Motion.stagger(binding.nearbyList, 0);
        if (map != null && center != null) {
            if (radiusCircle != null) {
                ValueAnimator a = ValueAnimator.ofFloat(old, newRadius);
                a.setDuration(Motion.enabled(requireContext()) ? 600 : 0);
                a.setInterpolator(Motion.spring(requireContext()));
                a.addUpdateListener(v -> {
                    if (radiusCircle != null) {
                        radiusCircle.setRadius((float) v.getAnimatedValue());
                    }
                });
                a.start();
            }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoomFor(newRadius)), 600, null);
            placeMarkers();
        }
    }

    private static float zoomFor(int radius) {
        if (radius <= 500) return 16.4f;
        if (radius <= 1000) return 15.5f;
        if (radius <= 3000) return 14f;
        return 13.2f;
    }

    private void renderCategories() {
        LayoutInflater inflater = getLayoutInflater();
        binding.categoryRow.removeAllViews();
        for (Category c : Category.values()) {
            ItemCategoryBinding row = ItemCategoryBinding.inflate(inflater, binding.categoryRow, false);
            row.icon.setImageResource(c.icon);
            row.label.setText(c.label);
            row.getRoot().setOnClickListener(v -> startActivity(BrowseActivity.intent(requireContext(), c)));
            binding.categoryRow.addView(row.getRoot());
        }
    }

    private void renderList() {
        List<Item> items = SampleData.itemsWithin(radius);
        int nearest = items.isEmpty() ? 0 : items.get(0).distanceM;
        binding.summary.setText(getString(R.string.discover_summary, radiusLabel(radius), items.size(), nearest));
        binding.nearbyList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < Math.min(items.size(), 6); i++) {
            Item item = items.get(i);
            ItemNearbyBinding row = ItemNearbyBinding.inflate(inflater, binding.nearbyList, false);
            Ui.thumb(row.thumb, item, 18, 0.2f);
            row.title.setText(item.title);
            row.tags.addView(Ui.tag(requireContext(), item.pickupLabel));
            if (item.status != ItemStatus.AVAILABLE) {
                TextView pill = new TextView(requireContext(), null, 0, R.style.T_Pill);
                Ui.statusPill(pill, item.status);
                row.tags.addView(pill);
            }
            row.distance.setText(Ui.distance(requireContext(), item.distanceM, 20));
            row.getRoot().setOnClickListener(v -> openItem(item));
            binding.nearbyList.addView(row.getRoot());
        }
    }

    private String radiusLabel(int r) {
        return r < 1000 ? getString(R.string.radius_500) : (r / 1000) + "km";
    }

    private void openItem(Item item) {
        startActivity(ItemDetailActivity.intent(requireContext(), item.id));
    }

    // ---------------------------------------------------------------- 地图与定位

    private void showPrivacyDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.privacy_title)
                .setMessage(R.string.privacy_message)
                .setCancelable(false)
                .setPositiveButton(R.string.privacy_agree, (dialog, which) -> {
                    AmapPrivacy.agree(requireContext());
                    initMap(null);
                })
                .setNegativeButton(R.string.privacy_disagree, (dialog, which) ->
                        showState(State.PRIVACY_REQUIRED, null))
                .show();
    }

    /** 只有在用户同意隐私政策后才能调用，否则高德 SDK 不工作。 */
    private void initMap(@Nullable Bundle mapState) {
        if (binding == null || mapView != null) {
            return;
        }
        AmapPrivacy.syncToSdk(requireContext());
        mapView = new MapView(requireContext());
        binding.mapContainer.addView(mapView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mapView.onCreate(mapState);
        if (getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume();
        }

        map = mapView.getMap();
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setLogoBottomMargin(Ui.dp(requireContext(), 300));
        map.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        map.setOnMyLocationChangeListener(this::onMyLocationChanged);
        map.setOnMarkerClickListener(marker -> {
            Object tag = marker.getObject();
            if (tag instanceof Item) {
                openItem((Item) tag);
                return true;
            }
            return false;
        });
        showState(State.LOCATING, null);
        requestLocation();
    }

    private void requestLocation() {
        if (hasLocationPermission()) {
            startLocating();
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS);
        }
    }

    private void startLocating() {
        if (map == null) {
            return;
        }
        LatLng mock = mockLocation();
        if (mock != null) {
            useMockLocation(mock);
            return;
        }
        showState(State.LOCATING, null);
        hasCentered = false;
        MyLocationStyle style = new MyLocationStyle()
                // 持续定位但不自动拉回视角：网络定位失败时，GPS 结果到达后仍能恢复；
                // 首次成功时由 onMyLocationChanged 把视角移到当前位置
                .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                .interval(LOCATION_INTERVAL_MS)
                .strokeColor(0x403E8EF7)
                .radiusFillColor(0x183E8EF7)
                .showMyLocation(true);
        map.setMyLocationStyle(style);
        map.setMyLocationEnabled(false);
        map.setMyLocationEnabled(true);
    }

    /** 本机 local.properties 配置的调试坐标；未配置或非 debug 包时返回 null。 */
    @Nullable
    private static LatLng mockLocation() {
        if (!BuildConfig.DEBUG || BuildConfig.MOCK_LOCATION.isEmpty()) {
            return null;
        }
        String[] parts = BuildConfig.MOCK_LOCATION.split(",");
        return new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
    }

    /** 不调用高德定位，直接把固定坐标当作当前位置（海外无法使用高德网络定位时调试用）。 */
    private void useMockLocation(LatLng mock) {
        map.setMyLocationEnabled(false);
        map.addMarker(new MarkerOptions().position(mock).title(getString(R.string.location_mock_marker))
                .icon(BitmapDescriptorFactory.fromView(myLocationDot())).anchor(0.5f, 0.5f));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mock, zoomFor(radius)));
        hasCentered = true;
        onCenterKnown(mock);
        showState(State.LOCATED, null);
    }

    /** 调试坐标用的“我的位置”蓝点。 */
    private View myLocationDot() {
        View dot = new View(requireContext());
        int size = Ui.dp(requireContext(), 20);
        dot.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        dot.setBackgroundResource(R.drawable.bg_my_location);
        return dot;
    }

    private void onMyLocationChanged(Location location) {
        if (binding == null || location == null) {
            return;
        }
        Bundle extras = location.getExtras();
        int errorCode = extras == null ? -1 : extras.getInt("errorCode", -1);
        if (errorCode == 0 && (location.getLatitude() != 0 || location.getLongitude() != 0)) {
            // 高德返回 GCJ-02 坐标，与接口约定一致，可直接作为检索中心
            if (!hasCentered) {
                hasCentered = true;
                LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(here, zoomFor(radius)));
                onCenterKnown(here);
            }
            if (state != State.LOCATED) {
                showState(State.LOCATED, null);
            }
        } else if (!hasCentered) {
            // 已经定位成功过的话，偶发失败不打扰用户，保留上次位置
            String info = extras == null ? null : extras.getString("errorInfo");
            showState(State.FAILED, getString(R.string.location_failed_detail, errorCode,
                    info == null ? getString(R.string.location_failed_unknown) : info));
        }
    }

    private void onCenterKnown(LatLng here) {
        center = here;
        if (radiusCircle != null) {
            radiusCircle.remove();
        }
        radiusCircle = map.addCircle(new CircleOptions().center(here).radius(radius)
                .fillColor(0x126A8A14).strokeColor(0xB06A8A14).strokeWidth(Ui.dp(requireContext(), 1.5f)));
        placeMarkers();
    }

    /** 在当前位置周围摆放半径内的物品标记，依次弹出。 */
    private void placeMarkers() {
        for (Marker m : markers) {
            m.remove();
        }
        markers.clear();
        List<Item> items = SampleData.itemsWithin(radius);
        double metersPerLat = 111320.0;
        double metersPerLng = 111320.0 * Math.cos(Math.toRadians(center.latitude));
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            ViewMapPinBinding pin = ViewMapPinBinding.inflate(inflater);
            pin.pinIcon.setImageResource(item.category.pinIcon);
            if (item.status != ItemStatus.AVAILABLE) {
                pin.pinBody.setBackgroundResource(R.drawable.bg_pin_gray);
            }
            if (item.id == SampleData.DEMO_ITEM_ID) {
                pin.pinLabel.setVisibility(View.VISIBLE);
                pin.pinLabel.setText(item.title.substring(0, 4) + " · " + item.distanceM + "m");
            }
            LatLng pos = new LatLng(center.latitude + item.northM / metersPerLat,
                    center.longitude + item.eastM / metersPerLng);
            Marker marker = map.addMarker(new MarkerOptions().position(pos).anchor(0.5f, 1f)
                    .icon(BitmapDescriptorFactory.fromView(pin.getRoot())));
            marker.setObject(item);
            if (Motion.enabled(requireContext())) {
                ScaleAnimation drop = new ScaleAnimation(0f, 1f, 0f, 1f);
                drop.setDuration(520 + i * 70L);
                drop.setInterpolator(Motion.spring(requireContext()));
                marker.setAnimation(drop);
                marker.startAnimation();
            }
            markers.add(marker);
        }
    }

    private void recenter() {
        Motion.bounce(binding.locateButton);
        if (map != null && center != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoomFor(radius)), 500, null);
        } else if (map != null) {
            requestLocation();
        } else {
            showPrivacyDialog();
        }
    }

    private void onStatusAction() {
        if (state == null) {
            return;
        }
        switch (state) {
            case PRIVACY_REQUIRED:
                showPrivacyDialog();
                break;
            case PERMISSION_DENIED:
                if (permissionPermanentlyDenied) {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null)));
                } else {
                    permissionLauncher.launch(LOCATION_PERMISSIONS);
                }
                break;
            case FAILED:
                requestLocation();
                break;
            default:
                break;
        }
    }

    private void showState(State newState, @Nullable String detail) {
        state = newState;
        if (binding == null) {
            return;
        }
        binding.statusCard.setVisibility(newState == State.LOCATED ? View.GONE : View.VISIBLE);
        binding.statusProgress.setVisibility(newState == State.LOCATING ? View.VISIBLE : View.GONE);
        binding.statusAction.setVisibility(newState == State.LOCATING ? View.GONE : View.VISIBLE);
        switch (newState) {
            case PRIVACY_REQUIRED:
                binding.statusText.setText(R.string.privacy_required);
                binding.statusAction.setText(R.string.privacy_review);
                break;
            case PERMISSION_DENIED:
                binding.statusText.setText(R.string.location_permission_denied);
                binding.statusAction.setText(permissionPermanentlyDenied
                        ? R.string.location_open_settings : R.string.location_grant);
                break;
            case LOCATING:
                binding.statusText.setText(R.string.location_loading);
                break;
            case FAILED:
                binding.statusText.setText(detail);
                binding.statusAction.setText(R.string.location_retry);
                break;
            default:
                break;
        }
    }

    private boolean hasLocationPermission() {
        for (String permission : LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED) {
                return true; // 用户只给“大致位置”也可以定位
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        // 从系统设置授权回来后自动重新定位
        if (state == State.PERMISSION_DENIED && hasLocationPermission()) {
            startLocating();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapState = new Bundle();
            mapView.onSaveInstanceState(mapState);
            outState.putBundle(KEY_MAP_STATE, mapState);
        }
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            map.setMyLocationEnabled(false);
            mapView.onDestroy();
            mapView = null;
            map = null;
        }
        markers.clear();
        radiusCircle = null;
        binding = null;
        super.onDestroyView();
    }
}
