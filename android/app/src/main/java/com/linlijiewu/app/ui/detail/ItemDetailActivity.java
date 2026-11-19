package com.linlijiewu.app.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.remote.ItemDetail;
import com.linlijiewu.app.databinding.ActivityItemDetailBinding;
import com.linlijiewu.app.util.ItemCategory;
import com.linlijiewu.app.util.ItemStatusStyle;

/**
 * 物品详情页（第 2 周）。当前由发布成功跳转 + Intent 传参进入；
 * 第 3 周检索接入后改为列表/地图标记点击，并按 id 调 GET /items/{id} 刷新。
 * 无 extras 启动时展示【测试】示例数据，便于独立验收界面。
 */
public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "itemId";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_COVER_URL = "coverUrl";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_PICKUP_LABEL = "pickupLabel";
    public static final String EXTRA_DISTANCE_M = "distanceM";
    public static final String EXTRA_OWNER_NICKNAME = "ownerNickname";

    public static void start(Context context, ItemDetail detail) {
        Intent intent = new Intent(context, ItemDetailActivity.class);
        intent.putExtra(EXTRA_ID, detail.getId());
        intent.putExtra(EXTRA_TITLE, detail.getTitle());
        intent.putExtra(EXTRA_CATEGORY, detail.getCategory());
        intent.putExtra(EXTRA_DESCRIPTION, detail.getDescription());
        intent.putExtra(EXTRA_COVER_URL, detail.getCoverUrl());
        intent.putExtra(EXTRA_STATUS, detail.getStatus());
        intent.putExtra(EXTRA_PICKUP_LABEL, detail.getPickupLabel());
        if (detail.getDistanceM() != null) {
            intent.putExtra(EXTRA_DISTANCE_M, detail.getDistanceM());
        }
        if (detail.getOwner() != null) {
            intent.putExtra(EXTRA_OWNER_NICKNAME, detail.getOwner().getNickname());
        }
        context.startActivity(intent);
    }

    private ActivityItemDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityItemDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_TITLE)) {
            bindDetail(intent);
        } else {
            bindDemo();
        }

        binding.borrowButton.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.detail_borrow_login_required,
                        Snackbar.LENGTH_LONG).show());
    }

    private void bindDetail(Intent intent) {
        binding.demoNotice.setVisibility(View.GONE);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String category = intent.getStringExtra(EXTRA_CATEGORY);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);
        String coverUrl = intent.getStringExtra(EXTRA_COVER_URL);
        String status = intent.getStringExtra(EXTRA_STATUS);
        String pickupLabel = intent.getStringExtra(EXTRA_PICKUP_LABEL);
        String owner = intent.getStringExtra(EXTRA_OWNER_NICKNAME);
        int distanceM = intent.getIntExtra(EXTRA_DISTANCE_M, -1);

        binding.detailTitle.setText(title);
        binding.detailCategory.setText(getString(ItemCategory.fromApiValue(
                category == null ? "OTHER" : category).labelRes));
        binding.detailDescription.setText(description == null || description.isEmpty()
                ? getString(R.string.detail_description_empty) : description);
        binding.pickupValue.setText(pickupLabel == null ? "—" : pickupLabel);
        binding.ownerValue.setText(owner == null ? "—" : owner);
        if (distanceM >= 0) {
            binding.detailDistance.setText(getString(R.string.detail_distance_m, distanceM));
            binding.detailDistance.setVisibility(View.VISIBLE);
        } else {
            binding.detailDistance.setVisibility(View.GONE);
        }
        bindStatus(status == null ? "AVAILABLE" : status);
        loadCover(coverUrl);
    }

    private void bindDemo() {
        binding.demoNotice.setVisibility(View.VISIBLE);
        binding.detailTitle.setText(R.string.demo_item_title);
        binding.detailCategory.setText(getString(ItemCategory.CAMPING.labelRes));
        binding.detailDescription.setText(R.string.demo_item_description);
        binding.pickupValue.setText(R.string.demo_item_pickup);
        binding.ownerValue.setText(R.string.demo_item_owner);
        binding.detailDistance.setText(getString(R.string.detail_distance_m, 486));
        bindStatus("AVAILABLE");
        binding.detailCover.setImageResource(R.drawable.ic_nav_discover);
    }

    private void bindStatus(String status) {
        binding.detailStatus.setText(getString(ItemStatusStyle.labelRes(status)));
        int color = getColor(ItemStatusStyle.colorRes(status));
        binding.detailStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
    }

    private void loadCover(String coverUrl) {
        if (coverUrl == null || coverUrl.isEmpty()) {
            binding.detailCover.setImageResource(R.drawable.ic_nav_discover);
            return;
        }
        String fullUrl = coverUrl.startsWith("http") ? coverUrl : resolveFileUrl(coverUrl);
        Glide.with(this).load(fullUrl).centerCrop().into(binding.detailCover);
    }

    /** 服务端返回的 /files/xxx 相对地址补全为服务器文件地址。 */
    static String resolveFileUrl(String relativePath) {
        String base = BuildConfig.SERVER_BASE_URL;
        if (base.endsWith("api/")) {
            base = base.substring(0, base.length() - "api/".length());
        }
        return base + relativePath;
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
