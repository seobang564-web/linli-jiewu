package com.linlijiewu.app.ui.publish;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.databinding.ActivityPublishBinding;
import com.linlijiewu.app.ui.auth.AuthActivity;
import com.linlijiewu.app.ui.common.*;
import com.linlijiewu.app.ui.item.ItemDetailActivity;
import com.linlijiewu.app.util.ImageCompressor;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

/** Authenticated publish/edit with private drafts, real pickup coordinates and sequential upload. */
public class PublishActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_ID = "edit_item_id";
    private ActivityPublishBinding binding;
    private Category category = Category.CAMPING;
    private String pickupLabel;
    private Double pickupLat;
    private Double pickupLng;
    private String imageId;
    private long editId;
    private boolean busy;
    private boolean published;
    private LinliApi api;
    private Call<?> activeCall;
    private LoadingDialog loading;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> pickupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PickupActivity.EXTRA_LAT) && data.hasExtra(PickupActivity.EXTRA_LNG)) {
                        pickupLabel = data.getStringExtra(PickupActivity.EXTRA_LABEL);
                        pickupLat = data.getDoubleExtra(PickupActivity.EXTRA_LAT, 0);
                        pickupLng = data.getDoubleExtra(PickupActivity.EXTRA_LNG, 0);
                        binding.pickupValue.setText(pickupLabel);
                        Motion.bounce(binding.pickupValue);
                        saveDraft();
                    }
                }
            });
    private final ActivityResultLauncher<PickVisualMediaRequest> photoLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::processPhoto);

    public static void open(Activity from) {
        from.startActivity(new Intent(from, PublishActivity.class));
        from.overridePendingTransition(R.anim.sheet_up, R.anim.scale_back);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, AuthActivity.class).putExtra("relogin", true));
            Toast.makeText(this, R.string.auth_login_needed, Toast.LENGTH_LONG).show();
            finish(); return;
        }
        editId = getIntent().getLongExtra(EXTRA_EDIT_ID, 0);
        api = ApiClient.authenticated(this);
        EdgeToEdge.enable(this);
        binding = ActivityPublishBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.topBar); Ui.padBottom(binding.bottomBar);
        binding.close.setOnClickListener(v -> finish());
        binding.titleInput.setText(""); binding.descInput.setText("");
        if (editId == 0) { restoreDraft(); }
        binding.pickupValue.setText(pickupLabel == null ? getString(R.string.publish_pickup_choose) : pickupLabel);
        renderCategories();
        counter(binding.titleInput, binding.titleCounter, R.string.publish_counter_title);
        counter(binding.descInput, binding.descCounter, R.string.publish_counter_desc);
        if (photoFile().isFile()) { previewPhoto(); }
        binding.photo.setOnClickListener(v -> photoLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.pickupRow.setOnClickListener(v -> pickupLauncher.launch(new Intent(this, PickupActivity.class)));
        binding.saveDraft.setEnabled(editId == 0);
        binding.saveDraft.setOnClickListener(v -> {
            saveDraft(); Toast.makeText(this, R.string.publish_draft_toast, Toast.LENGTH_SHORT).show();
        });
        binding.submit.setOnClickListener(v -> submit());
        if (editId != 0) { binding.submit.setText(R.string.edit_save); loadEdit(); }
        Motion.stagger(binding.content, 200);
    }

    private SharedPreferences draft() {
        return getSharedPreferences("publish_draft_" + Session.userId(this), MODE_PRIVATE);
    }
    private File photoFile() { return new File(getFilesDir(), "publish-photo-" + Session.userId(this) + "-" + editId + ".jpg"); }
    private void restoreDraft() {
        SharedPreferences d = draft();
        binding.titleInput.setText(d.getString("title", ""));
        binding.descInput.setText(d.getString("desc", ""));
        try { category = Category.valueOf(d.getString("category", "CAMPING")); }
        catch (IllegalArgumentException ignored) { category = Category.CAMPING; }
        pickupLabel = d.getString("pickup", null);
        imageId = d.getString("image_id", null);
        if (d.contains("lat") && d.contains("lng")) {
            try {
                pickupLat = Double.valueOf(d.getString("lat", "")); pickupLng = Double.valueOf(d.getString("lng", ""));
            } catch (NumberFormatException ignored) { pickupLat = null; pickupLng = null; }
        }
    }
    private void loadEdit() {
        setBusy(true);
        var call = api.item(editId); activeCall = call;
        call.enqueue(new ApiCallback<>(this, item -> {
            if (!item.isMine) { showError(getString(R.string.auth_login_needed)); finish(); return; }
            binding.titleInput.setText(item.title); binding.descInput.setText(item.description);
            category = Category.valueOf(item.category); renderCategories();
            pickupLat = item.pickupLat; pickupLng = item.pickupLng; pickupLabel = item.pickupLabel;
            binding.pickupValue.setText(pickupLabel);
            if (item.coverUrl != null && item.coverUrl.matches("/files/[a-f0-9]{32}\\.png")) {
                if (photoFile().isFile()) { imageId = null; previewPhoto(); }
                else {
                    imageId = item.coverUrl.substring(7, 39);
                    Glide.with(this).load(FileUrls.resolve(BuildConfig.SERVER_BASE_URL, item.coverUrl)).into(binding.photoImage);
                }
            }
        }, message -> { showError(message); binding.submit.setEnabled(false); }, () -> setBusy(false)));
    }
    private void processPhoto(Uri uri) {
        if (uri == null) { return; }
        imageId = null;
        saveDraft();
        setBusy(true);
        File target = photoFile();
        worker.execute(() -> {
            try {
                byte[] bytes = ImageCompressor.compress(getApplicationContext(), uri).jpeg;
                try (var output = new java.io.FileOutputStream(target)) { output.write(bytes); }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) { return; }
                    imageId = null; setBusy(false); previewPhoto(); saveDraft();
                    Toast.makeText(this, R.string.publish_photo_ready, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) { setBusy(false); showError(getString(R.string.publish_photo_failed)); }
                });
            }
        });
    }
    private void previewPhoto() {
        binding.photoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(photoFile()).signature(new com.bumptech.glide.signature.ObjectKey(photoFile().lastModified()))
                .into(binding.photoImage);
    }
    private void submit() {
        if (busy) { return; }
        if (!Session.isLoggedIn(this)) { showError(getString(R.string.auth_login_needed)); return; }
        if (binding.titleInput.getText().toString().trim().isEmpty()) {
            binding.titleInput.setError(getString(R.string.publish_err_title)); return;
        }
        if (pickupLat == null || pickupLng == null || pickupLabel == null || pickupLabel.trim().isEmpty()) {
            showError(getString(R.string.publish_pickup_needed)); return;
        }
        saveDraft(); setBusy(true);
        loading = new LoadingDialog(this); loading.show(R.string.loading_publish);
        if (photoFile().isFile() && imageId == null) {
            var part = MultipartBody.Part.createFormData("file", "publish.jpg",
                    RequestBody.create(photoFile(), MediaType.parse("image/jpeg")));
            var call = api.upload(part); activeCall = call;
            call.enqueue(new ApiCallback<>(this, uploaded -> {
                imageId = uploaded.imageId; saveDraft(); sendItem();
            }, message -> { finishRequest(); showError(message); }, () -> { }));
        } else { sendItem(); }
    }
    private void sendItem() {
        ItemData.Write request = new ItemData.Write();
        request.title = binding.titleInput.getText().toString().trim();
        request.category = category.name(); request.description = binding.descInput.getText().toString();
        request.imageId = imageId; request.pickupLat = pickupLat; request.pickupLng = pickupLng; request.pickupLabel = pickupLabel;
        var call = editId == 0 ? api.publish(request) : api.edit(editId, request); activeCall = call;
        call.enqueue(new ApiCallback<>(this, item -> {
            published = true;
            if (editId == 0) { draft().edit().clear().apply(); }
            photoFile().delete();
            startActivity(ItemDetailActivity.intent(this, item.id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }, this::showError, this::finishRequest));
    }
    private void finishRequest() { setBusy(false); if (loading != null) { loading.dismiss(); } }
    private void setBusy(boolean value) {
        busy = value; binding.submit.setEnabled(!value); binding.photo.setEnabled(!value);
        binding.pickupRow.setEnabled(!value); binding.titleInput.setEnabled(!value); binding.descInput.setEnabled(!value);
        binding.saveDraft.setEnabled(!value && editId == 0);
        for (int i = 0; i < binding.categoryGroup.getChildCount(); i++) { binding.categoryGroup.getChildAt(i).setEnabled(!value); }
    }
    private void showError(String message) { Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show(); }
    private void renderCategories() {
        binding.categoryGroup.removeAllViews();
        for (Category c : Category.values()) {
            TextView chip = new TextView(this, null, 0, R.style.Chip); chip.setText(c.label);
            chip.setCompoundDrawablesRelativeWithIntrinsicBounds(c.icon, 0, 0, 0);
            chip.setCompoundDrawablePadding(Ui.dp(this, 5));
            chip.getCompoundDrawablesRelative()[0].setBounds(0, 0, Ui.dp(this, 16), Ui.dp(this, 16));
            chip.setCompoundDrawablesRelative(chip.getCompoundDrawablesRelative()[0], null, null, null);
            chip.setSelected(c == category);
            chip.setOnClickListener(v -> { category = c; renderCategories(); saveDraft(); });
            binding.categoryGroup.addView(chip);
        }
    }
    private void counter(TextView input, TextView counter, int format) {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { counter.setText(getString(format, s.length())); }
        };
        input.addTextChangedListener(watcher); watcher.afterTextChanged(input.getEditableText());
    }
    private void saveDraft() {
        if (editId != 0 || binding == null || published) { return; }
        var editor = draft().edit().putString("title", binding.titleInput.getText().toString())
                .putString("desc", binding.descInput.getText().toString()).putString("category", category.name())
                .putString("pickup", pickupLabel).putString("image_id", imageId);
        if (pickupLat != null && pickupLng != null) { editor.putString("lat", pickupLat.toString()).putString("lng", pickupLng.toString()); }
        editor.apply();
        binding.draftStatus.setText(getString(R.string.publish_draft_saved, new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date())));
    }
    @Override protected void onPause() { super.onPause(); if (!published) { saveDraft(); } }
    @Override protected void onDestroy() {
        if (activeCall != null) { activeCall.cancel(); }
        if (loading != null) { loading.dismiss(); }
        worker.shutdownNow(); super.onDestroy();
    }
    @Override public void finish() { super.finish(); overridePendingTransition(R.anim.scale_forward, R.anim.sheet_down); }
}
