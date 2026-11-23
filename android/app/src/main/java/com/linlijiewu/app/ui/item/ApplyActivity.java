package com.linlijiewu.app.ui.item;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.ui.common.Session;
import com.linlijiewu.app.ui.auth.AuthActivity;
import com.google.gson.Gson;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;
import retrofit2.Call;
import com.linlijiewu.app.databinding.ActivityApplyBinding;
import com.linlijiewu.app.ui.common.LoadingDialog;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.orders.OrderDetailActivity;
import java.util.Calendar;
import java.util.Locale;

/** 申请借用：选择归还日期与时间、给物主留言。提交对应 POST /borrows（带客户端生成的 requestId）。 */
public class ApplyActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "item_id";
    private static final String[] WEEK = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    private static final String[] TIMES = {"12:00", "18:00", "21:00"};

    private ActivityApplyBinding binding;
    private int dayIndex = 3;
    private String time = "18:00";
    private final String[] dayLabels = new String[5];
    private long itemId, accountId;
    private Calendar baseDay;
    private BorrowData.Create pending;
    private Call<?> active;
    private boolean submitting;
    private TextView retryHint;

    public static void open(Context context, long itemId) {
        context.startActivity(Session.isLoggedIn(context) ? intent(context, itemId)
                : new Intent(context, AuthActivity.class).putExtra("relogin", true));
    }

    public static Intent intent(Context context, long itemId) {
        return new Intent(context, ApplyActivity.class).putExtra(EXTRA_ID, itemId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) { open(this, getIntent().getLongExtra(EXTRA_ID, 0)); finish(); return; }
        accountId = Session.userId(this); itemId = getIntent().getLongExtra(EXTRA_ID, 0);
        baseDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        baseDay.set(Calendar.HOUR_OF_DAY, 0); baseDay.set(Calendar.MINUTE, 0); baseDay.set(Calendar.SECOND, 0); baseDay.set(Calendar.MILLISECOND, 0);
        if (savedInstanceState != null) { baseDay.setTimeInMillis(savedInstanceState.getLong("base_day", baseDay.getTimeInMillis()));
            dayIndex = savedInstanceState.getInt("day_index", 3); time = savedInstanceState.getString("time", "18:00"); }
        try { pending = new Gson().fromJson(prefs().getString(Long.toString(itemId), null), BorrowData.Create.class); }
        catch (RuntimeException exception) { prefs().edit().remove(Long.toString(itemId)).apply(); }
        EdgeToEdge.enable(this);
        binding = ActivityApplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.top.topBarRoot);
        Ui.padBottom(binding.bottomBar);
        Ui.back(this, binding.top.back);
        binding.top.title.setText(R.string.apply_title);

        binding.submit.setEnabled(false);
        retryHint = Ui.message(this, getString(R.string.borrow_pending_retry), this::resetPending);
        binding.content.addView(retryHint, 0);
        retryHint.setVisibility(pending == null ? android.view.View.GONE : android.view.View.VISIBLE);
        if (pending != null) { binding.note.setText(pending.note); binding.note.setEnabled(false); }
        var call = ApiClient.authenticated(this).item(itemId); active = call;
        call.enqueue(new ApiCallback<>(this, item -> {
            Ui.thumb(binding.thumb, item, 18, 0); binding.itemTitle.setText(item.title);
            binding.tags.addView(Ui.tag(this, item.pickupLabel));
            boolean available = !item.isMine && "AVAILABLE".equals(item.status);
            binding.submit.setEnabled(available || pending != null);
            if (!available && pending == null) { Toast.makeText(this, R.string.borrow_item_unavailable, Toast.LENGTH_LONG).show(); }
        }, message -> { binding.submit.setEnabled(pending != null); error(message); }, () -> { }));

        renderDays();
        renderTimes();
        TextWatcher counter = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                binding.noteCounter.setText(getString(R.string.apply_counter, s.length()));
            }
        };
        binding.note.addTextChangedListener(counter);
        counter.afterTextChanged(binding.note.getText());

        binding.submit.setOnClickListener(v -> submit());
        Motion.stagger(binding.content, 140);
    }

    private void renderDays() {
        Calendar cal = (Calendar) baseDay.clone();
        binding.dayRow.removeAllViews();
        for (int i = 0; i < 5; i++) {
            String top = i == 0 ? getString(R.string.day_today) : i == 1 ? getString(R.string.day_tomorrow)
                    : WEEK[cal.get(Calendar.DAY_OF_WEEK) - 1];
            String date = String.format(Locale.ROOT, "%02d-%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            dayLabels[i] = date;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setBackgroundResource(R.drawable.bg_day);
            cell.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            cell.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(this, R.animator.press_scale));
            TextView a = new TextView(this, null, 0, R.style.T);
            a.setText(top);
            a.setTextSize(13);
            TextView b = new TextView(this, null, 0, R.style.T_Sub);
            b.setText(date);
            b.setTextSize(12);
            b.setPadding(0, Ui.dp(this, 3), 0, 0);
            cell.addView(a);
            cell.addView(b);
            boolean on = i == dayIndex;
            cell.setSelected(on);
            a.setTextColor(ContextCompat.getColor(this, on ? R.color.olive_deep : R.color.ink));
            final int index = i;
            cell.setOnClickListener(v -> {
                if (pending != null || submitting) { return; }
                dayIndex = index;
                renderDays();
                Motion.bounce(binding.dayRow.getChildAt(index));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) {
                lp.setMarginStart(Ui.dp(this, 6));
            }
            binding.dayRow.addView(cell, lp);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        updateSummary();
    }

    private void renderTimes() {
        binding.timeRow.removeAllViews();
        for (String t : TIMES) {
            TextView chip = new TextView(this, null, 0, R.style.Chip);
            chip.setText(t);
            chip.setSelected(t.equals(time));
            chip.setOnClickListener(v -> {
                if (pending != null || submitting) { return; }
                time = t;
                renderTimes();
                updateSummary();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(this, 8));
            binding.timeRow.addView(chip, lp);
        }
        TextView other = new TextView(this, null, 0, R.style.Chip);
        other.setText(R.string.apply_other_time);
        other.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_clock_small, 0, 0, 0);
        other.setOnClickListener(v -> {
            if (pending != null || submitting) { return; }
            new android.app.TimePickerDialog(this, (picker, hour, minute) -> {
                time = String.format(Locale.ROOT, "%02d:%02d", hour, minute); renderTimes(); updateSummary();
            }, 18, 0, true).show();
        });
        binding.timeRow.addView(other);
    }

    private void updateSummary() {
        if (pending != null) { binding.dueSummary.setText(Ui.date(pending.dueAt) + " UTC"); return; }
        String days = dayIndex == 0 ? "<1" : String.valueOf(dayIndex);
        binding.dueSummary.setText(getString(R.string.apply_summary, days, dayLabels[dayIndex], time));
    }

    private android.content.SharedPreferences prefs() { return getSharedPreferences("borrow_pending_" + accountId, MODE_PRIVATE); }
    private void submit() {
        if (submitting) { return; }
        if (Session.userId(this) != accountId || !Session.isLoggedIn(this)) { error(getString(R.string.auth_login_needed)); return; }
        if (pending == null) {
            Calendar due = (Calendar) baseDay.clone(); due.add(Calendar.DAY_OF_MONTH, dayIndex);
            String[] parts = time.split(":"); due.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0])); due.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            if (due.getTimeInMillis() <= System.currentTimeMillis()) { error(getString(R.string.borrow_past_due)); return; }
            pending = new BorrowData.Create(); pending.itemId = itemId; pending.requestId = UUID.randomUUID().toString();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT); format.setTimeZone(TimeZone.getTimeZone("UTC"));
            pending.dueAt = format.format(due.getTime()); pending.note = binding.note.getText().toString();
            prefs().edit().putString(Long.toString(itemId), new Gson().toJson(pending)).apply();
        }
        submitting = true; binding.submit.setEnabled(false); binding.note.setEnabled(false);
        retryHint.setVisibility(android.view.View.VISIBLE);
        var call = ApiClient.authenticated(this).borrow(pending); active = call;
        call.enqueue(new ApiCallback<>(this, order -> {
            prefs().edit().remove(Long.toString(itemId)).apply();
            startActivity(OrderDetailActivity.intent(this, order.id)); finish();
        }, this::error, () -> { submitting = false; binding.submit.setEnabled(true); }));
    }
    private void resetPending() {
        if (submitting) { return; }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.borrow_reset_title).setMessage(R.string.borrow_reset_message)
                .setNegativeButton(R.string.order_view, (d, w) -> startActivity(com.linlijiewu.app.ui.main.HomeActivity.intent(this,
                        com.linlijiewu.app.ui.main.HomeActivity.TAB_ORDERS)))
                .setPositiveButton(R.string.borrow_reset, (d, w) -> {
                    pending = null; prefs().edit().remove(Long.toString(itemId)).apply(); retryHint.setVisibility(android.view.View.GONE);
                    binding.note.setEnabled(true); updateSummary();
                }).show();
    }
    private void error(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); if (baseDay != null) { out.putLong("base_day", baseDay.getTimeInMillis()); }
        out.putInt("day_index", dayIndex); out.putString("time", time);
    }
    @Override protected void onDestroy() { if (active != null) { active.cancel(); } super.onDestroy(); }
}
