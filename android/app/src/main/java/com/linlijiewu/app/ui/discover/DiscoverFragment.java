package com.linlijiewu.app.ui.discover;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.FragmentDiscoverBinding;
import com.linlijiewu.app.util.AmapPrivacy;

/**
 * 发现页：高德地图 + 当前位置。
 * 流程：隐私合规弹窗 → 创建 MapView → 申请定位权限 → 持续定位，首次成功时把视角移到当前位置。
 * 搜索框、半径控件、物品标记和列表联动在此基础上继续接入。
 */
public class DiscoverFragment extends Fragment {
    private static final String KEY_MAP_STATE = "map_state";
    private static final float DEFAULT_ZOOM = 16f;
    private static final long LOCATION_INTERVAL_MS = 5000;
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private enum State { PRIVACY_REQUIRED, PERMISSION_DENIED, LOCATING, LOCATED, FAILED }

    private FragmentDiscoverBinding binding;
    private MapView mapView;
    private AMap map;
    private State state;
    private boolean permissionPermanentlyDenied;
    private boolean hasCentered;

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
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusCard, (card, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
            params.bottomMargin = bars.bottom + getResources().getDimensionPixelSize(R.dimen.discover_card_margin);
            card.setLayoutParams(params);
            return insets;
        });
        // 地图避开状态栏，否则右上角定位按钮会被遮挡
        ViewCompat.setOnApplyWindowInsetsListener(binding.mapContainer, (container, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            container.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });
        binding.statusAction.setOnClickListener(v -> onStatusAction());

        if (AmapPrivacy.isAgreed(requireContext())) {
            initMap(savedInstanceState == null ? null : savedInstanceState.getBundle(KEY_MAP_STATE));
        } else {
            showState(State.PRIVACY_REQUIRED, null);
            showPrivacyDialog();
        }
    }

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
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        map.setOnMyLocationChangeListener(this::onMyLocationChanged);
        // 先切出“需要同意隐私政策”状态，权限弹窗期间显示定位中
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
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.clear();
        map.addMarker(new MarkerOptions().position(mock).title(getString(R.string.location_mock_marker)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mock, DEFAULT_ZOOM));
        hasCentered = true;
        showState(State.LOCATED, null);
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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
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
        binding = null;
        super.onDestroyView();
    }
}
