package com.linlijiewu.app.ui.item;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.databinding.ActivityMyItemsBinding;
import com.linlijiewu.app.ui.common.Session;
import com.linlijiewu.app.ui.common.Ui;
import retrofit2.Call;

/** Minimal paged entry for authenticated publications and shared item IDs. */
public class MyItemsActivity extends AppCompatActivity {
    private ActivityMyItemsBinding binding;
    private Call<?> call;
    private int page = 1;
    private long loaded;
    private boolean busy;
    public static void openId(Context context) {
        EditText field = new EditText(context); field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setHint(R.string.item_id_hint);
        var dialog = new MaterialAlertDialogBuilder(context).setTitle(R.string.item_open_id).setView(field)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnShowListener(d -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long id = Long.parseLong(field.getText().toString().trim());
                if (id <= 0) { throw new NumberFormatException(); }
                context.startActivity(ItemDetailActivity.intent(context, id)); dialog.dismiss();
            } catch (NumberFormatException exception) { field.setError(context.getString(R.string.item_id_invalid)); }
        }));
        dialog.show();
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        binding = ActivityMyItemsBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        Ui.padTop(binding.top.topBarRoot); Ui.padBottom(binding.getRoot());
        binding.top.title.setText(R.string.my_items_title); Ui.back(this, binding.top.back);
        binding.openId.setOnClickListener(v -> openId(this)); binding.more.setOnClickListener(v -> load());
    }
    @Override protected void onResume() {
        super.onResume();
        if (call != null) { call.cancel(); }
        page = 1; loaded = 0; busy = false; binding.list.removeAllViews(); load();
    }
    private void load() {
        if (busy) { return; }
        if (!Session.isLoggedIn(this)) { binding.state.setText(R.string.auth_login_needed); binding.more.setVisibility(View.GONE); return; }
        busy = true; binding.more.setEnabled(false); binding.state.setText(R.string.item_load);
        var request = ApiClient.authenticated(this).mine(page, 20); call = request;
        request.enqueue(new ApiCallback<>(this, data -> {
            if (data.list == null) { binding.state.setText(R.string.network_response_failed); return; }
            for (ItemData item : data.list) {
                TextView row = new TextView(this, null, 0, R.style.T_Title);
                int padding = Ui.dp(this, 20); row.setPadding(padding, padding, padding, padding);
                row.setText(item.title + "\n" + getString(R.string.item_number, item.id));
                row.setBackgroundResource(R.drawable.bg_card);
                row.setOnClickListener(v -> startActivity(ItemDetailActivity.intent(this, item.id)));
                binding.list.addView(row);
            }
            loaded += data.list.size(); page++;
            binding.state.setText(loaded == 0 ? getString(R.string.my_items_empty) : "");
            binding.more.setVisibility(loaded < data.total && !data.list.isEmpty() ? View.VISIBLE : View.GONE);
        }, message -> binding.state.setText(message), () -> { busy = false; binding.more.setEnabled(true); }));
    }
    @Override protected void onDestroy() { if (call != null) { call.cancel(); } super.onDestroy(); }
}
