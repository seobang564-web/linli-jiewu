package com.linlijiewu.app.ui.browse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.databinding.*;
import com.linlijiewu.app.ui.common.*;
import com.linlijiewu.app.ui.item.ApplyActivity;
import com.linlijiewu.app.ui.item.ItemDetailActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;

public class BrowseActivity extends AppCompatActivity {
    private static final String EXTRA_CATEGORY = "category";
    private ActivityBrowseBinding binding;
    private Category selected;
    private final List<ItemData> items = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable search = () -> load(true);
    private Call<?> active;
    private int sequence, page, radius = 1000;
    private long total;
    private boolean loading;

    public static Intent intent(Context c, Category category) { return new Intent(c, BrowseActivity.class).putExtra(EXTRA_CATEGORY, category.name()); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        binding = ActivityBrowseBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        Ui.padTop(binding.header); Ui.padBottom(binding.list); Ui.back(this, binding.back);
        String category = state == null ? getIntent().getStringExtra(EXTRA_CATEGORY) : state.getString(EXTRA_CATEGORY);
        selected = category == null ? null : Ui.category(category);
        radius = state == null ? SearchLocation.radius(this) : state.getInt("radius", 1000);
        binding.toMap.setOnClickListener(v -> finish());
        binding.search.setOnClickListener(v -> { binding.searchInput.requestFocus();
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(binding.searchInput, 0); });
        binding.radiusSelector.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setItems(new String[]{"500 m", "1 km", "3 km", "5 km"}, (d, index) -> {
                    radius = new int[]{500, 1000, 3000, 5000}[index]; load(true);
                }).show());
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(search); sequence++; if (active != null) { active.cancel(); }
                handler.postDelayed(search, 300);
            }
            public void afterTextChanged(Editable s) { }
        });
        renderSide();
    }
    @Override protected void onResume() { super.onResume(); load(true); }

    private void renderSide() {
        binding.sideList.removeAllViews(); addSide(null);
        for (Category category : Category.values()) { addSide(category); }
    }
    private void addSide(Category category) {
        ItemSideCategoryBinding row = ItemSideCategoryBinding.inflate(getLayoutInflater(), binding.sideList, false);
        row.label.setText(category == null ? R.string.browse_all : category.label);
        row.icon.setImageResource(category == null ? R.drawable.ic_home : category.icon);
        row.getRoot().setSelected(selected == category); row.label.setSelected(selected == category);
        row.icon.setVisibility(selected == category ? View.VISIBLE : View.GONE);
        row.getRoot().setOnClickListener(v -> { selected = category; renderSide(); load(true); });
        binding.sideList.addView(row.getRoot());
    }
    private void load(boolean reset) {
        if (!SearchLocation.available(this)) {
            binding.list.removeAllViews(); binding.list.addView(Ui.message(this, getString(R.string.nearby_locating), this::finish)); return;
        }
        if (!reset && loading) { return; }
        int query = reset ? ++sequence : sequence;
        if (active != null) { active.cancel(); }
        if (reset) { page = 0; total = 0; items.clear(); }
        loading = true; render();
        binding.radiusSelector.setText(getString(R.string.nearby_radius, radius));
        var call = ApiClient.authenticated(this).nearby(SearchLocation.lat(this), SearchLocation.lng(this), radius,
                binding.searchInput.getText().toString().trim(), selected == null ? null : selected.name(), page + 1, 20);
        active = call;
        call.enqueue(new ApiCallback<>(this, data -> {
            if (query != sequence) { return; } loading = false; page = data.page; total = data.total;
            if (data.list != null) { items.addAll(data.list); } render();
        }, message -> {
            if (query != sequence) { return; } loading = false; render();
            binding.list.addView(Ui.message(this, getString(R.string.nearby_retry, message), () -> load(reset)));
        }, () -> { }));
    }
    private void render() {
        binding.list.removeAllViews();
        if (loading) { binding.list.addView(Ui.message(this, getString(R.string.nearby_loading), null)); }
        else if (items.isEmpty()) { binding.list.addView(Ui.message(this, getString(R.string.nearby_empty), () -> load(true))); }
        for (ItemData item : items) {
            ItemBrowseBinding row = ItemBrowseBinding.inflate(getLayoutInflater(), binding.list, false);
            Ui.thumb(row.thumb, item, 43, 0); row.title.setText(item.title);
            row.tags.addView(Ui.tag(this, getString(Ui.category(item.category).label)));
            row.tags.addView(Ui.tag(this, item.pickupLabel)); row.desc.setText(item.pickupLabel);
            row.distance.setText(Ui.distance(this, item.distanceM == null ? 0 : item.distanceM, 20));
            row.borrow.setOnClickListener(v -> ApplyActivity.open(this, item.id));
            row.getRoot().setOnClickListener(v -> startActivity(ItemDetailActivity.intent(this, item.id)));
            binding.list.addView(row.getRoot());
        }
        if (!loading && items.size() < total) { binding.list.addView(Ui.message(this, getString(R.string.load_more), () -> load(false))); }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); if (selected != null) { out.putString(EXTRA_CATEGORY, selected.name()); } out.putInt("radius", radius);
    }
    @Override protected void onDestroy() {
        sequence++; if (active != null) { active.cancel(); } handler.removeCallbacksAndMessages(null); super.onDestroy();
    }
}
