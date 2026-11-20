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
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.sample.SampleData;
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

    public static Intent intent(Context context, long itemId) {
        return new Intent(context, ApplyActivity.class).putExtra(EXTRA_ID, itemId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityApplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.top.topBarRoot);
        Ui.padBottom(binding.bottomBar);
        Ui.back(this, binding.top.back);
        binding.top.title.setText(R.string.apply_title);

        Item item = SampleData.item(getIntent().getLongExtra(EXTRA_ID, SampleData.DEMO_ITEM_ID));
        Ui.thumb(binding.thumb, item, 18, 0.2f);
        binding.title.setText(item.title);
        binding.tags.addView(Ui.tag(this, item.pickupLabel));
        TextView dist = new TextView(this, null, 0, R.style.T_Sub);
        dist.setText(getString(R.string.distance_m, item.distanceM));
        binding.tags.addView(dist);

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

        binding.submit.setOnClickListener(v -> LoadingDialog.run(this, R.string.loading_apply, () -> {
            startActivity(OrderDetailActivity.intent(this, 1024));
            finish();
        }));
        Motion.stagger(binding.content, 140);
    }

    private void renderDays() {
        Calendar cal = Calendar.getInstance();
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
        other.setOnClickListener(v -> Ui.comingSoon(this));
        binding.timeRow.addView(other);
    }

    private void updateSummary() {
        String days = dayIndex == 0 ? "<1" : String.valueOf(dayIndex);
        binding.dueSummary.setText(getString(R.string.apply_summary, days, dayLabels[dayIndex], time));
    }
}
