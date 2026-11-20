package com.linlijiewu.app.ui.publish;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.ActivityPublishBinding;
import com.linlijiewu.app.ui.common.LoadingDialog;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.item.ItemDetailActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 发布闲置：从底部弹出。提交顺序为 POST /images → POST /items。
 * 草稿暂存在 SharedPreferences，计划中的 SQLite 草稿表完成后替换。
 */
public class PublishActivity extends AppCompatActivity {
    private static final String PREFS = "publish_draft";

    private ActivityPublishBinding binding;
    private Category category = Category.CAMPING;
    private String pickupLabel;
    private final ActivityResultLauncher<Intent> pickupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pickupLabel = result.getData().getStringExtra(PickupActivity.EXTRA_LABEL);
                    binding.pickupValue.setText(pickupLabel);
                    Motion.bounce(binding.pickupValue);
                    markSaved();
                }
            });

    public static void open(Activity from) {
        from.startActivity(new Intent(from, PublishActivity.class));
        from.overridePendingTransition(R.anim.sheet_up, R.anim.scale_back);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPublishBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.topBar);
        Ui.padBottom(binding.bottomBar);
        binding.close.setOnClickListener(v -> finish());

        SharedPreferences draft = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (draft.contains("title")) {
            binding.titleInput.setText(draft.getString("title", ""));
            binding.descInput.setText(draft.getString("desc", ""));
            category = Category.valueOf(draft.getString("category", Category.CAMPING.name()));
        }
        pickupLabel = draft.getString("pickup", SampleData.PICKUP_POIS[0][0]);
        binding.pickupValue.setText(pickupLabel);
        renderCategories();
        counter(binding.titleInput, binding.titleCounter, R.string.publish_counter_title);
        counter(binding.descInput, binding.descCounter, R.string.publish_counter_desc);
        markSaved();

        binding.photo.setOnClickListener(v -> Ui.comingSoon(this));
        binding.pickupRow.setOnClickListener(v -> pickupLauncher.launch(new Intent(this, PickupActivity.class)));
        binding.saveDraft.setOnClickListener(v -> {
            saveDraft();
            Toast.makeText(this, R.string.publish_draft_toast, Toast.LENGTH_SHORT).show();
        });
        binding.submit.setOnClickListener(v -> submit());
        Motion.stagger(binding.content, 200);
    }

    private void renderCategories() {
        binding.categoryGroup.removeAllViews();
        for (Category c : Category.values()) {
            TextView chip = new TextView(this, null, 0, R.style.Chip);
            chip.setText(c.label);
            chip.setCompoundDrawablesRelativeWithIntrinsicBounds(c.icon, 0, 0, 0);
            chip.setCompoundDrawablePadding(Ui.dp(this, 5));
            chip.getCompoundDrawablesRelative()[0].setBounds(0, 0, Ui.dp(this, 16), Ui.dp(this, 16));
            chip.setCompoundDrawablesRelative(chip.getCompoundDrawablesRelative()[0], null, null, null);
            chip.setSelected(c == category);
            chip.setOnClickListener(v -> {
                category = c;
                renderCategories();
                Motion.bounce(binding.categoryGroup.getChildAt(c.ordinal()));
                markSaved();
            });
            binding.categoryGroup.addView(chip);
        }
    }

    private void counter(TextView input, TextView counter, int format) {
        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                counter.setText(getString(format, s.length()));
            }
        };
        input.addTextChangedListener(w);
        w.afterTextChanged(input.getEditableText());
    }

    private void submit() {
        if (binding.titleInput.getText().toString().trim().isEmpty()) {
            binding.titleInput.setError(getString(R.string.publish_err_title));
            binding.titleInput.requestFocus();
            return;
        }
        LoadingDialog.run(this, R.string.loading_publish, () -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            startActivity(ItemDetailActivity.intent(this, SampleData.DEMO_ITEM_ID));
            finish();
        });
    }

    private void saveDraft() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("title", binding.titleInput.getText().toString())
                .putString("desc", binding.descInput.getText().toString())
                .putString("category", category.name())
                .putString("pickup", pickupLabel)
                .apply();
        markSaved();
    }

    private void markSaved() {
        String time = new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date());
        binding.draftStatus.setText(getString(R.string.publish_draft_saved, time));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing()) {
            saveDraft();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.scale_forward, R.anim.sheet_down);
    }
}
