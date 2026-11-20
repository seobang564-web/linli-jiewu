package com.linlijiewu.app.ui.item;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.model.ItemStatus;
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.databinding.ActivityItemDetailBinding;
import com.linlijiewu.app.ui.common.*;
import com.linlijiewu.app.ui.auth.AuthActivity;
import com.linlijiewu.app.ui.publish.PublishActivity;
import retrofit2.Call;

/** Real item details; borrowing and chat remain scheduled for later weeks. */
public class ItemDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "item_id";
    private ActivityItemDetailBinding binding;
    private long id;
    private ItemData item;
    private Call<?> activeCall;
    private LoadingDialog loading;

    public static Intent intent(Context context, long id) {
        return new Intent(context, ItemDetailActivity.class).putExtra(EXTRA_ID, id);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        binding = ActivityItemDetailBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        Ui.padTop(binding.topBar); Ui.padBottom(binding.bottomBar); Ui.back(this, binding.back);
        id = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.more.setOnClickListener(v -> menu());
        binding.favorite.setOnClickListener(v -> { v.setSelected(!v.isSelected()); Motion.bounce(v); });
        binding.contact.setEnabled(false); binding.apply.setEnabled(false);
        binding.scroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, x, y, ox, oy) -> {
                    if (Motion.enabled(this)) {
                        binding.hero.setTranslationY(-y * 0.35f);
                        binding.hero.setAlpha(1f - Math.min(y, 300) / 600f);
                    }
                });
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        if (id <= 0) { error(getString(R.string.item_id_invalid)); return; }
        if (activeCall != null) { activeCall.cancel(); }
        if (loading != null) { loading.dismiss(); }
        loading = new LoadingDialog(this); loading.show(R.string.item_load);
        var call = ApiClient.authenticated(this).item(id); activeCall = call;
        call.enqueue(new ApiCallback<>(this, this::render, this::error, loading::dismiss));
    }
    private void render(ItemData value) {
        item = value;
        Category category = Category.valueOf(item.category);
        ItemStatus status = ItemStatus.valueOf(item.status);
        binding.hero.setBackgroundColor(ContextCompat.getColor(this, R.color.tone_olive));
        Glide.with(this).load(FileUrls.resolve(BuildConfig.SERVER_BASE_URL, item.coverUrl))
                .placeholder(category.illustration).error(category.illustration).into(binding.heroImage);
        binding.title.setText(item.title); Ui.statusPill(binding.status, status);
        binding.tags.removeAllViews(); binding.tags.addView(Ui.tag(this, getString(category.label)));
        binding.tags.addView(Ui.tag(this, getString(R.string.item_number, item.id)));
        binding.distance.setText(item.distanceM == null ? item.pickupLabel
                : getString(R.string.detail_distance, item.distanceM, Math.max(1, Math.round(item.distanceM / 70f))));
        binding.description.setText(item.description); binding.pickupLabel.setText(item.pickupLabel);
        binding.pickupAddress.setText(getString(R.string.item_coordinates, item.pickupLat, item.pickupLng));
        binding.miniPinIcon.setImageResource(category.pinIcon);
        String nick = item.owner == null || item.owner.nickname == null ? "" : item.owner.nickname;
        binding.ownerAvatar.setText(nick.isEmpty() ? "" : nick.substring(0, 1)); binding.ownerName.setText(nick);
        binding.ownerMeta.setText(getString(R.string.item_created, item.createdAt == null ? "" : item.createdAt.substring(0, Math.min(10, item.createdAt.length()))));
        binding.navigate.setOnClickListener(v -> Ui.comingSoon(this));
        binding.ownerRow.setOnClickListener(v -> later(R.string.week2_chat_later));
        binding.contact.setEnabled(true); binding.contact.setText(item.isMine ? R.string.item_manage : R.string.detail_chat);
        binding.contact.setOnClickListener(v -> { if (item.isMine) { menu(); } else { later(R.string.week2_chat_later); } });
        boolean available = !item.isMine && status == ItemStatus.AVAILABLE;
        binding.apply.setEnabled(available); binding.apply.setAlpha(available ? 1f : 0.45f);
        binding.apply.setOnClickListener(v -> later(R.string.week2_borrow_later));
        Motion.stagger(binding.content, 80);
    }
    private void later(int message) {
        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, AuthActivity.class).putExtra("relogin", true));
            Toast.makeText(this, R.string.auth_login_needed, Toast.LENGTH_LONG).show();
        } else { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    }
    private void menu() {
        if (item == null) { return; }
        boolean canEdit = item.isMine && !"OFFLINE".equals(item.status);
        String[] options = canEdit ? new String[]{getString(R.string.edit_item), getString(R.string.item_offline), getString(R.string.item_share)}
                : new String[]{getString(R.string.item_share)};
        new MaterialAlertDialogBuilder(this).setTitle(item.title).setItems(options, (dialog, which) -> {
            if (canEdit && which == 0) {
                startActivity(new Intent(this, PublishActivity.class).putExtra(PublishActivity.EXTRA_EDIT_ID, item.id));
            } else if (canEdit && which == 1) {
                new MaterialAlertDialogBuilder(this).setMessage(R.string.item_offline_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (d, w) -> offline()).show();
            } else {
                startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, getString(R.string.item_share_text, item.title, item.id)), getString(R.string.item_share)));
            }
        }).show();
    }
    private void offline() {
        loading = new LoadingDialog(this); loading.show(R.string.item_load);
        var call = ApiClient.authenticated(this).offline(id); activeCall = call;
        call.enqueue(new ApiCallback<>(this, value -> {
            render(value); Toast.makeText(this, R.string.item_offline_done, Toast.LENGTH_SHORT).show();
        }, this::error, loading::dismiss));
    }
    private void error(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_INDEFINITE).setAction(R.string.item_retry, v -> load()).show();
    }
    @Override protected void onDestroy() {
        if (activeCall != null) { activeCall.cancel(); }
        if (loading != null) { loading.dismiss(); }
        super.onDestroy();
    }
}
