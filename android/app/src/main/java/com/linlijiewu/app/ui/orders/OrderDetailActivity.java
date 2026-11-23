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
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.ui.common.Session;
import com.linlijiewu.app.ui.auth.AuthActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import retrofit2.Call;
import com.linlijiewu.app.databinding.ActivityOrderDetailBinding;
import com.linlijiewu.app.ui.chat.ChatActivity;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;

/**
 * 订单详情（GET /borrows/{id} + /logs）：大号状态 + 五步进度，接受后露出对方联系方式，底部是订单记录。
 * 仅渲染真实接口与按当前身份可执行的状态操作。
 */
public class OrderDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "order_id";
    private static final BorrowStatus[] STEPS = {BorrowStatus.PENDING, BorrowStatus.ACCEPTED,
            BorrowStatus.BORROWING, BorrowStatus.RETURNING, BorrowStatus.COMPLETED};

    private ActivityOrderDetailBinding binding;
    private Call<?> detailCall, logCall, actionCall;
    private long id;
    private int sequence;
    private boolean changing;
    private BorrowData order;

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

        id = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.contactOwner.setVisibility(View.GONE);
        binding.call.setVisibility(View.GONE); binding.navigate.setVisibility(View.GONE);
        binding.copyContact.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("contact", binding.contactValue.getText()));
            Toast.makeText(this, R.string.order_copied, Toast.LENGTH_SHORT).show();
        });
        binding.cancel.setOnClickListener(v -> load());
        Motion.stagger(binding.content, 140);
    }

    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        if (changing) { return; }
        if (!Session.isLoggedIn(this)) { startActivity(new Intent(this, AuthActivity.class).putExtra("relogin", true)); finish(); return; }
        if (id <= 0) { error(getString(R.string.item_id_invalid)); return; }
        int query = ++sequence;
        if (detailCall != null) { detailCall.cancel(); } if (logCall != null) { logCall.cancel(); }
        binding.stateActions.removeAllViews(); binding.contactValue.setText(R.string.order_contact_hidden); binding.copyContact.setEnabled(false);
        binding.statusTitle.setText(R.string.item_load);
        var call = ApiClient.authenticated(this).borrow(id); detailCall = call;
        call.enqueue(new ApiCallback<>(this, value -> { if (query == sequence) { render(value); loadLogs(query); } }, this::error, () -> { }));
    }
    private void render(BorrowData value) {
        order = value;
        BorrowStatus status = BorrowStatus.valueOf(order.status);
        binding.statusTitle.setText(getString(status.label) + (order.overdue ? " · " + getString(R.string.order_overdue) : ""));
        int hint;
        switch (status) {
            case PENDING: hint = R.string.order_hint_pending; break;
            case ACCEPTED: hint = R.string.order_hint_accepted; break;
            case BORROWING: hint = R.string.order_hint_borrowing; break;
            case RETURNING: hint = R.string.order_hint_returning; break;
            case COMPLETED: hint = R.string.order_hint_completed; break;
            default: hint = R.string.order_hint_cancelled;
        }
        binding.statusHint.setText(hint);
        renderSteps(status == BorrowStatus.CANCELLED ? 0 : status.ordinal());
        binding.steps.setVisibility(status == BorrowStatus.CANCELLED ? View.GONE : View.VISIBLE);
        binding.stepTrack.setVisibility(status == BorrowStatus.CANCELLED ? View.GONE : View.VISIBLE);
        binding.stepFill.setVisibility(status == BorrowStatus.CANCELLED ? View.GONE : View.VISIBLE);
        binding.pickupLabel.setText(order.item.pickupLabel);
        binding.pickupAddress.setText(getString(R.string.item_coordinates, order.item.pickupLat, order.item.pickupLng));
        BorrowData.Person peer = order.owner.id == Session.userId(this) ? order.borrower : order.owner;
        binding.contactLabel.setText(order.owner.id == Session.userId(this) ? R.string.order_contact_borrower : R.string.order_contact_owner);
        boolean visible = !"PENDING".equals(order.status) && !"CANCELLED".equals(order.status);
        binding.contactValue.setText(!visible ? getString(R.string.order_contact_hidden)
                : peer.contact == null || peer.contact.isEmpty() ? getString(R.string.order_contact_empty) : peer.contact);
        binding.copyContact.setEnabled(visible && peer.contact != null && !peer.contact.isEmpty());
        binding.copyContact.setAlpha(binding.copyContact.isEnabled() ? 1f : 0.35f);
        Ui.thumb(binding.thumb, order.item, 14, 0); binding.itemTitle.setText(order.item.title); binding.snapshot.setText(R.string.order_snapshot_real);
        binding.infoRows.removeAllViews(); addInfo(R.string.order_due, Ui.date(order.dueAt));
        addInfo(R.string.order_created, Ui.date(order.createdAt)); addInfo(R.string.order_note, order.note);
        addInfo(R.string.order_no, Long.toString(order.id));
        if (order.cancelReason != null) { addInfo(R.string.order_action_reason, order.cancelReason); }
        renderActions();
    }
    private void renderActions() {
        binding.stateActions.removeAllViews();
        for (String action : BorrowActions.forUser(order, Session.userId(this))) {
            MaterialButton button = new MaterialButton(this); button.setText(Ui.borrowActionLabel(action));
            button.setEnabled(!changing); button.setOnClickListener(v -> confirm(action)); binding.stateActions.addView(button);
        }
    }
    private void confirm(String action) {
        if (changing) { return; }
        EditText reason = new EditText(this); reason.setHint(R.string.order_action_reason);
        reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
        reason.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.order_action_confirm, getString(Ui.borrowActionLabel(action))))
                .setNegativeButton(android.R.string.cancel, null);
        boolean needsReason = "cancel".equals(action) || "reject".equals(action) || "deny-return".equals(action);
        if (needsReason) { dialog.setView(reason); }
        dialog.setPositiveButton(android.R.string.ok, (d, w) -> change(action, needsReason ? reason.getText().toString() : null)).show();
    }
    private void change(String action, String reason) {
        if (changing) { return; } changing = true; renderActions();
        var call = ApiClient.authenticated(this).borrowAction(id, action, new BorrowData.Action(reason)); actionCall = call;
        call.enqueue(new ApiCallback<>(this, value -> {
            Toast.makeText(this, R.string.order_action_done, Toast.LENGTH_SHORT).show(); load();
        }, message -> { error(message); load(); }, () -> { changing = false; }));
    }
    private void loadLogs(int query) {
        binding.timeline.removeAllViews();
        var call = ApiClient.authenticated(this).borrowLogs(id); logCall = call;
        call.enqueue(new ApiCallback<>(this, logs -> {
            if (query != sequence) { return; } binding.timeline.removeAllViews();
            for (int i = logs.size() - 1; i >= 0; i--) {
                BorrowData.Log log = logs.get(i);
                String actor = log.actor == null ? getString(R.string.order_actor_system) : log.actor.nickname;
                String from = log.fromStatus == null ? getString(R.string.order_initial) : getString(BorrowStatus.valueOf(log.fromStatus).label);
                String to = getString(BorrowStatus.valueOf(log.toStatus).label);
                addLog(getString(R.string.order_log_real, actor, from, to, log.reason == null ? "" : " · " + log.reason), Ui.date(log.createdAt), i == logs.size() - 1);
            }
            if (logs.isEmpty()) { binding.timeline.addView(Ui.message(this, getString(R.string.order_logs_empty), null)); }
        }, message -> { if (query == sequence) { binding.timeline.addView(Ui.message(this, getString(R.string.nearby_retry, message), () -> loadLogs(query))); } }, () -> { }));
    }
    private void error(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() {
        sequence++; if (detailCall != null) { detailCall.cancel(); } if (logCall != null) { logCall.cancel(); }
        if (actionCall != null) { actionCall.cancel(); } super.onDestroy();
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
