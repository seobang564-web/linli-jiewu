package com.linlijiewu.app.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.BorrowStatus;
import com.linlijiewu.app.data.model.ChatMessage;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.ActivityChatBinding;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.orders.OrderDetailActivity;
import java.util.List;

/**
 * 聊天：顶部钉住物品与订单状态，订单状态变化以系统消息插入。
 * 发送走 POST /conversations/{id}/messages，新消息由 WebSocket 推送；原型阶段发送后模拟对方“正在输入”并回复。
 */
public class ChatActivity extends AppCompatActivity {
    private static final String EXTRA_ITEM = "item_id";

    private ActivityChatBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String peerInitial;
    private View typing;

    public static Intent intent(Context context, long itemId) {
        return new Intent(context, ChatActivity.class).putExtra(EXTRA_ITEM, itemId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.header);
        Ui.padBottom(binding.inputBar);
        Ui.back(this, binding.back);

        Item item = SampleData.item(getIntent().getLongExtra(EXTRA_ITEM, SampleData.DEMO_ITEM_ID));
        peerInitial = item.ownerName.substring(0, 1);
        binding.peerName.setText(item.ownerName);
        Ui.thumb(binding.itemThumb, item, 12, 0.18f);
        binding.itemTitle.setText(item.title);
        binding.itemMeta.setText(getString(R.string.chat_item_meta, getString(BorrowStatus.ACCEPTED.label)));
        binding.itemCard.setOnClickListener(v -> startActivity(OrderDetailActivity.intent(this, 1024)));
        binding.more.setOnClickListener(v -> Ui.comingSoon(this));

        List<ChatMessage> history = SampleData.demoChat();
        for (ChatMessage m : history) {
            binding.messages.addView(view(m));
        }
        scrollToEnd();
        Motion.stagger(binding.messages, 160);

        binding.send.setOnClickListener(v -> send());
        binding.input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });
        breathe(binding.onlineDot);
    }

    private void send() {
        String text = binding.input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        binding.input.setText("");
        Motion.bounce(binding.send);
        View mine = view(new ChatMessage(ChatMessage.Type.MINE, text));
        binding.messages.addView(mine);
        pop(mine, true);
        scrollToEnd();
        handler.postDelayed(() -> {
            typing = typingView();
            binding.messages.addView(typing);
            pop(typing, false);
            scrollToEnd();
        }, 500);
        handler.postDelayed(() -> {
            if (typing != null) {
                binding.messages.removeView(typing);
                typing = null;
            }
            View reply = view(new ChatMessage(ChatMessage.Type.PEER, getString(R.string.chat_auto_reply)));
            binding.messages.addView(reply);
            pop(reply, false);
            scrollToEnd();
        }, 1800);
    }

    private View view(ChatMessage m) {
        switch (m.type) {
            case TIME: {
                TextView t = new TextView(this, null, 0, R.style.T_Sub);
                t.setText(m.text);
                t.setTextSize(11.5f);
                t.setGravity(Gravity.CENTER);
                return wrap(t, Gravity.CENTER_HORIZONTAL);
            }
            case EVENT: {
                TextView t = new TextView(this, null, 0, R.style.T);
                t.setText(m.text);
                t.setTextSize(12);
                t.setTextColor(ContextCompat.getColor(this, R.color.ink_2));
                t.setBackgroundResource(R.drawable.bg_sys);
                t.setPadding(Ui.dp(this, 12), Ui.dp(this, 5), Ui.dp(this, 12), Ui.dp(this, 5));
                t.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_order_small, 0, 0, 0);
                t.setCompoundDrawablePadding(Ui.dp(this, 6));
                t.setGravity(Gravity.CENTER_VERTICAL);
                return wrap(t, Gravity.CENTER_HORIZONTAL);
            }
            case MINE:
                return bubble(m.text, true);
            default:
                return bubble(m.text, false);
        }
    }

    private View bubble(String text, boolean mine) {
        TextView b = new TextView(this, null, 0, R.style.T);
        b.setText(text);
        b.setTextSize(14.5f);
        b.setLineSpacing(0, 1.3f);
        b.setTextIsSelectable(true);
        b.setMaxWidth(Math.round(getResources().getDisplayMetrics().widthPixels * 0.68f));
        b.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        b.setBackgroundResource(mine ? R.drawable.bg_bubble_me : R.drawable.bg_bubble_you);
        b.setTextColor(ContextCompat.getColor(this, mine ? R.color.surface : R.color.ink));
        if (mine) {
            return wrap(b, Gravity.END);
        }
        return withAvatar(b);
    }

    private View withAvatar(View content) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.BOTTOM);
        TextView avatar = new TextView(this, null, 0, R.style.Avatar);
        avatar.setText(peerInitial);
        avatar.setTextSize(13);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32));
        alp.setMarginEnd(Ui.dp(this, 8));
        row.addView(avatar, alp);
        row.addView(content);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        return row;
    }

    private View wrap(View child, int gravity) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(gravity);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        row.addView(child);
        return row;
    }

    private View typingView() {
        LinearLayout dots = new LinearLayout(this);
        dots.setBackgroundResource(R.drawable.bg_bubble_you);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        dots.setPadding(Ui.dp(this, 16), Ui.dp(this, 15), Ui.dp(this, 16), Ui.dp(this, 15));
        for (int i = 0; i < 3; i++) {
            View d = new View(this);
            d.setBackgroundResource(R.drawable.bg_dot);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 6), Ui.dp(this, 6));
            if (i > 0) {
                lp.setMarginStart(Ui.dp(this, 5));
            }
            dots.addView(d, lp);
        }
        Motion.dots(dots);
        return withAvatar(dots);
    }

    /** 新消息从说话一侧弹出。 */
    private void pop(View row, boolean mine) {
        if (!Motion.enabled(this)) {
            return;
        }
        row.post(() -> {
            row.setPivotX(mine ? row.getWidth() : 0);
            row.setPivotY(row.getHeight());
            row.setScaleX(0.6f);
            row.setScaleY(0.6f);
            row.setAlpha(0f);
            row.setTranslationY(Ui.dp(this, 12));
            row.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0).setDuration(420)
                    .setInterpolator(Motion.spring(this)).start();
        });
    }

    private void breathe(View dot) {
        if (!Motion.enabled(this)) {
            return;
        }
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.35f);
        a.setDuration(900);
        a.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        a.start();
    }

    private void scrollToEnd() {
        binding.scroll.post(() -> binding.scroll.smoothScrollTo(0, binding.messages.getHeight()));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
