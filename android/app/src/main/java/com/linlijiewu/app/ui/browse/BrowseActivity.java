package com.linlijiewu.app.ui.browse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.model.ItemStatus;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.ActivityBrowseBinding;
import com.linlijiewu.app.databinding.ItemBrowseBinding;
import com.linlijiewu.app.databinding.ItemSideCategoryBinding;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.item.ApplyActivity;
import com.linlijiewu.app.ui.item.ItemDetailActivity;
import java.util.List;

/** 分类浏览：左侧分类，右侧按距离排序的列表。数据来自 GET /items?category=…（目前为示例数据）。 */
public class BrowseActivity extends AppCompatActivity {
    private static final String EXTRA_CATEGORY = "category";

    private ActivityBrowseBinding binding;
    private Category selected;

    public static Intent intent(Context context, Category category) {
        return new Intent(context, BrowseActivity.class).putExtra(EXTRA_CATEGORY, category.name());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityBrowseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.header);
        Ui.padBottom(binding.list);
        Ui.back(this, binding.back);
        binding.toMap.setOnClickListener(v -> finish());
        binding.search.setOnClickListener(v -> Ui.comingSoon(this));

        String cat = getIntent().getStringExtra(EXTRA_CATEGORY);
        selected = cat == null ? null : Category.valueOf(cat);
        renderSide();
        renderList(false);
    }

    private void renderSide() {
        binding.sideList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        addSide(inflater, null, getString(R.string.browse_all), R.drawable.ic_home);
        for (Category c : Category.values()) {
            addSide(inflater, c, getString(c.label), c.icon);
        }
    }

    private void addSide(LayoutInflater inflater, Category category, String label, int icon) {
        ItemSideCategoryBinding row = ItemSideCategoryBinding.inflate(inflater, binding.sideList, false);
        boolean on = category == selected;
        row.label.setText(label);
        row.getRoot().setSelected(on);
        row.label.setSelected(on);
        row.label.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        row.icon.setImageResource(icon);
        row.icon.setVisibility(on ? View.VISIBLE : View.GONE);
        row.getRoot().setOnClickListener(v -> {
            if (category == selected) {
                return;
            }
            selected = category;
            renderSide();
            renderList(true);
        });
        binding.sideList.addView(row.getRoot());
        if (on) {
            Motion.popIn(row.icon, 0);
        }
    }

    private void renderList(boolean animate) {
        binding.list.removeAllViews();
        TextView header = new TextView(this, null, 0, R.style.T_Body);
        header.setText(selected == null ? getString(R.string.browse_nearest) : getString(selected.label));
        header.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), 0, Ui.dp(this, 4));
        binding.list.addView(header);

        List<Item> items = SampleData.itemsIn(selected);
        if (items.isEmpty()) {
            binding.list.addView(emptyView());
        }
        LayoutInflater inflater = getLayoutInflater();
        for (Item item : items) {
            ItemBrowseBinding row = ItemBrowseBinding.inflate(inflater, binding.list, false);
            Ui.thumb(row.thumb, item, 43, 0.2f);
            row.title.setText(item.title);
            row.tags.addView(Ui.tag(this, getString(item.category.label)));
            row.tags.addView(Ui.tag(this, item.pickupLabel));
            row.desc.setText(item.description);
            row.distance.setText(Ui.distance(this, item.distanceM, 20));
            boolean available = item.status == ItemStatus.AVAILABLE;
            if (!available) {
                row.status.setVisibility(View.VISIBLE);
                Ui.statusPill(row.status, item.status);
                row.borrow.setBackgroundResource(R.drawable.bg_circle_disabled);
                row.borrow.setEnabled(false);
            } else {
                row.borrow.setOnClickListener(v -> startActivity(ApplyActivity.intent(this, item.id)));
            }
            row.getRoot().setOnClickListener(v -> startActivity(ItemDetailActivity.intent(this, item.id)));
            binding.list.addView(row.getRoot());
        }
        binding.listScroll.scrollTo(0, 0);
        Motion.stagger(binding.list, animate ? 0 : 160);
    }

    private View emptyView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        box.setPadding(0, Ui.dp(this, 50), 0, Ui.dp(this, 50));
        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.mascot_empty);
        mascot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        box.addView(mascot, new LinearLayout.LayoutParams(Ui.dp(this, 100), Ui.dp(this, 100)));
        TextView text = new TextView(this, null, 0, R.style.T_Sub);
        text.setText(R.string.browse_empty);
        text.setPadding(0, Ui.dp(this, 10), 0, 0);
        box.addView(text, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Motion.bob(mascot);
        return box;
    }
}
