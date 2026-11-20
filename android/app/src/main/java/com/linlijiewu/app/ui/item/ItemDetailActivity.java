package com.linlijiewu.app.ui.item;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.model.ItemStatus;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.ActivityItemDetailBinding;
import com.linlijiewu.app.ui.chat.ChatActivity;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;

/** 物品详情：头图视差、交接点卡片、物主信息；底部“联系物主”（POST /conversations）与“申请借用”。 */
public class ItemDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "item_id";

    public static Intent intent(Context context, long itemId) {
        return new Intent(context, ItemDetailActivity.class).putExtra(EXTRA_ID, itemId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivityItemDetailBinding b = ActivityItemDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Ui.padTop(b.topBar);
        Ui.padBottom(b.bottomBar);
        Ui.back(this, b.back);

        Item item = SampleData.item(getIntent().getLongExtra(EXTRA_ID, SampleData.DEMO_ITEM_ID));
        b.hero.setBackgroundColor(ContextCompat.getColor(this, item.tone));
        b.heroImage.setImageResource(item.category.illustration);
        b.title.setText(item.title);
        Ui.statusPill(b.status, item.status);
        b.tags.addView(Ui.tag(this, getString(item.category.label)));
        b.tags.addView(Ui.tag(this, getString(R.string.detail_handover)));
        b.distance.setText(getString(R.string.detail_distance, item.distanceM, Math.max(1, Math.round(item.distanceM / 70f))));
        b.description.setText(item.description);
        b.pickupLabel.setText(item.pickupLabel);
        b.pickupAddress.setText(R.string.browse_place);
        b.miniPinIcon.setImageResource(item.category.pinIcon);
        ((android.view.View) b.miniMap.getParent()).setClipToOutline(true);
        b.ownerAvatar.setText(item.ownerName.substring(0, 1));
        b.ownerName.setText(item.ownerName);
        b.ownerMeta.setText(getString(R.string.detail_owner_meta, "2026-11-16", "楠苑"));

        b.navigate.setOnClickListener(v -> Ui.comingSoon(this));
        b.favorite.setOnClickListener(v -> {
            v.setSelected(!v.isSelected());
            Motion.bounce(v);
        });
        b.ownerRow.setOnClickListener(v -> startActivity(ChatActivity.intent(this, item.id)));
        b.contact.setOnClickListener(v -> startActivity(ChatActivity.intent(this, item.id)));
        boolean available = item.status == ItemStatus.AVAILABLE;
        b.apply.setEnabled(available);
        b.apply.setAlpha(available ? 1f : 0.45f);
        b.apply.setOnClickListener(v -> startActivity(ApplyActivity.intent(this, item.id)));

        // 头图视差：内容上滑时头图以 0.35 倍速上移并逐渐变暗
        b.scroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, x, y, ox, oy) -> {
                    if (!Motion.enabled(this)) {
                        return;
                    }
                    b.hero.setTranslationY(-y * 0.35f);
                    b.hero.setAlpha(1f - Math.min(y, 300) / 600f);
                });
        if (Motion.enabled(this)) {
            b.heroImage.setScaleX(0.86f);
            b.heroImage.setScaleY(0.86f);
            b.heroImage.setAlpha(0f);
            b.heroImage.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(160).setDuration(700)
                    .setInterpolator(Motion.easeOut(this)).start();
        }
        Motion.stagger(b.content, 180);
    }
}
