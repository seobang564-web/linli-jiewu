package com.linlijiewu.app.ui.orders;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.BorrowStatus;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.ActivityOrderDetailBinding;
import com.linlijiewu.app.ui.chat.ChatActivity;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;

/**
 * 订单详情（GET /borrows/{id} + /logs）：大号状态 + 五步进度，接受后露出对方联系方式，底部是订单记录。
 * 示例展示的是借入方视角的“待交接”订单。
 */
public class OrderDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "order_id";
    private static final BorrowStatus[] STEPS = {BorrowStatus.PENDING, BorrowStatus.ACCEPTED,
            BorrowStatus.BORROWING, BorrowStatus.RETURNING, BorrowStatus.COMPLETED};

    private ActivityOrderDetailBinding binding;

    public static Intent intent(Context context, long orderId) {
        return new Intent(context, OrderDetailActivity.class).putExtra(EXTRA_ID, orderId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.top.topBarRoot);
        Ui.padBottom(binding.content);
        Ui.back(this, binding.top.back);
        binding.top.title.setText(R.string.order_detail_title);

        Item item = SampleData.item(SampleData.DEMO_ITEM_ID);
        BorrowStatus status = BorrowStatus.ACCEPTED;
        binding.statusTitle.setText(status.label);
        renderSteps(status.ordinal());
        binding.pickupLabel.setText(item.pickupLabel);
        binding.pickupAddress.setText(SampleData.PICKUP_POIS[0][1]);
        binding.contactValue.setText("微信 shuyu_01");
        Ui.thumb(binding.thumb, item, 14, 0.2f);
        binding.title.setText(item.title);
        binding.snapshot.setText(getString(R.string.order_snapshot, getString(item.category.label)));
        addInfo(R.string.order_due, "2026-11-22 18:00");
        addInfo(R.string.order_created, "2026-11-19 20:12:36");
        addInfo(R.string.order_note, getString(R.string.apply_note_default));
        addInfo(R.string.order_no, String.valueOf(getIntent().getLongExtra(EXTRA_ID, 1024)));
        addLog("物主 舒煜 接受了申请", "2026-11-19 20:30", true);
        addLog("你 提交了借用申请", "2026-11-19 20:12", false);

        binding.contactOwner.setOnClickListener(v -> startActivity(ChatActivity.intent(this, item.id)));
        binding.copyContact.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("contact", binding.contactValue.getText()));
            Toast.makeText(this, R.string.order_copied, Toast.LENGTH_SHORT).show();
        });
        binding.call.setOnClickListener(v -> Ui.comingSoon(this));
        binding.navigate.setOnClickListener(v -> Ui.comingSoon(this));
        binding.cancel.setOnClickListener(v -> Ui.comingSoon(this));
        Motion.stagger(binding.content, 140);
    }

    private void renderSteps(int current) {
        binding.steps.removeAllViews();
        for (int i = 0; i < STEPS.length; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            View dot = new View(this);
            int size = Ui.dp(this, i == current ? 24 : 16);
            dot.setBackgroundResource(i < current ? R.drawable.bg_step_done : i == current ? R.drawable.bg_step_cur : R.drawable.bg_step_todo);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(size, size);
            dlp.topMargin = i == current ? 0 : Ui.dp(this, 4);
            dlp.bottomMargin = i == current ? Ui.dp(this, 2) : Ui.dp(this, 6);
            cell.addView(dot, dlp);
            TextView label = new TextView(this, null, 0, R.style.T);
            label.setText(STEPS[i].label);
            label.setTextSize(11.5f);
            label.setTextColor(ContextCompat.getColor(this, i <= current ? R.color.ink : R.color.sub));
            cell.addView(label);
            binding.steps.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            if (i == current) {
                Motion.popIn(dot, 500);
            }
        }
        // 进度线从左向右长出来
        binding.steps.post(() -> {
            View first = binding.steps.getChildAt(0);
            View last = binding.steps.getChildAt(STEPS.length - 1);
            View cur = binding.steps.getChildAt(current);
            float start = first.getX() + first.getWidth() / 2f;
            float end = last.getX() + last.getWidth() / 2f;
            binding.stepTrack.setX(start);
            binding.stepFill.setX(start);
            android.view.ViewGroup.LayoutParams tl = binding.stepTrack.getLayoutParams();
            tl.width = (int) (end - start);
            binding.stepTrack.setLayoutParams(tl);
            int target = (int) (cur.getX() - first.getX());
            if (!Motion.enabled(this)) {
                setFill(target);
                return;
            }
            android.animation.ValueAnimator a = android.animation.ValueAnimator.ofInt(0, target);
            a.setStartDelay(300);
            a.setDuration(700);
            a.setInterpolator(Motion.easeOut(this));
            a.addUpdateListener(v -> setFill((int) v.getAnimatedValue()));
            a.start();
        });
    }

    private void setFill(int width) {
        android.view.ViewGroup.LayoutParams lp = binding.stepFill.getLayoutParams();
        lp.width = width;
        binding.stepFill.setLayoutParams(lp);
    }

    private void addInfo(int label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        TextView l = new TextView(this, null, 0, R.style.T_Sub);
        l.setText(label);
        l.setTextSize(14);
        TextView v = new TextView(this, null, 0, R.style.T);
        v.setText(value);
        v.setTextSize(14);
        v.setGravity(Gravity.END);
        v.setFontFeatureSettings("tnum");
        row.addView(l, new LinearLayout.LayoutParams(Ui.dp(this, 76), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        binding.infoRows.addView(row);
    }

    private void addLog(String text, String time, boolean latest) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        View dot = new View(this);
        dot.setBackgroundResource(latest ? R.drawable.bg_step_done : R.drawable.bg_dot_gray);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(this, 10), Ui.dp(this, 10));
        dlp.topMargin = Ui.dp(this, 5);
        dlp.setMarginEnd(Ui.dp(this, 12));
        row.addView(dot, dlp);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView a = new TextView(this, null, 0, R.style.T);
        a.setText(text);
        a.setTextSize(14);
        TextView b = new TextView(this, null, 0, R.style.T_Sub);
        b.setText(time);
        b.setTextSize(12);
        b.setPadding(0, Ui.dp(this, 3), 0, 0);
        col.addView(a);
        col.addView(b);
        row.addView(col);
        binding.timeline.addView(row);
    }
}
