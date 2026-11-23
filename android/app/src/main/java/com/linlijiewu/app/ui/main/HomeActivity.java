package com.linlijiewu.app.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.ActivityHomeBinding;
import com.linlijiewu.app.ui.chat.MessagesFragment;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.discover.DiscoverFragment;
import com.linlijiewu.app.ui.orders.OrdersFragment;
import com.linlijiewu.app.ui.profile.ProfileFragment;
import com.linlijiewu.app.ui.publish.PublishActivity;

/**
 * 主框架：发现 / 订单 / 消息 / 我的 四个 Tab 常驻（切换时只显示隐藏，地图不会重建），中间凸起按钮进入发布。
 */
public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_TAB = "tab";
    public static final String TAB_DISCOVER = "discover";
    public static final String TAB_ORDERS = "orders";
    public static final String TAB_MESSAGES = "messages";
    public static final String TAB_PROFILE = "profile";
    private static final String KEY_TAB = "current_tab";

    private ActivityHomeBinding binding;
    private String current;

    public static Intent intent(Context context, String tab) {
        return new Intent(context, HomeActivity.class).putExtra(EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.navBadge.setVisibility(View.GONE);
        Ui.padBottom(binding.navRow);

        binding.navDiscover.setOnClickListener(v -> select(TAB_DISCOVER, true));
        binding.navOrders.setOnClickListener(v -> select(TAB_ORDERS, true));
        binding.navMessages.setOnClickListener(v -> select(TAB_MESSAGES, true));
        binding.navProfile.setOnClickListener(v -> select(TAB_PROFILE, true));
        binding.navPublishSlot.setOnClickListener(v -> PublishActivity.open(this));
        binding.navPublish.setOnClickListener(v -> PublishActivity.open(this));

        String tab = savedInstanceState != null ? savedInstanceState.getString(KEY_TAB, TAB_DISCOVER)
                : getIntent().getStringExtra(EXTRA_TAB);
        select(tab == null ? TAB_DISCOVER : tab, false);
        if (savedInstanceState == null && Motion.enabled(this)) {
            binding.navBar.setTranslationY(Ui.dp(this, 90));
            binding.navBar.animate().translationY(0).setStartDelay(200).setDuration(560)
                    .setInterpolator(Motion.easeOut(this)).start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String tab = intent.getStringExtra(EXTRA_TAB);
        if (tab != null) {
            select(tab, true);
        }
        if (intent.hasExtra("orders_owner")) {
            Fragment orders = getSupportFragmentManager().findFragmentByTag(TAB_ORDERS);
            if (orders instanceof OrdersFragment) { ((OrdersFragment) orders).showRole(intent.getBooleanExtra("orders_owner", false)); }
        }
    }

    private void select(String tab, boolean animate) {
        if (tab.equals(current)) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        if (animate && Motion.enabled(this)) {
            tx.setCustomAnimations(R.anim.tab_enter, R.anim.tab_exit);
        }
        for (String t : new String[]{TAB_DISCOVER, TAB_ORDERS, TAB_MESSAGES, TAB_PROFILE}) {
            Fragment f = fm.findFragmentByTag(t);
            if (t.equals(tab)) {
                if (f == null) {
                    tx.add(R.id.tabContainer, create(t), t);
                } else {
                    tx.show(f);
                }
            } else if (f != null) {
                tx.hide(f);
            }
        }
        tx.commit();
        current = tab;
        updateNav(animate);
    }

    private Fragment create(String tab) {
        switch (tab) {
            case TAB_ORDERS:
                OrdersFragment orders = new OrdersFragment();
                Bundle args = new Bundle(); args.putBoolean("owner", getIntent().getBooleanExtra("orders_owner", false));
                orders.setArguments(args); return orders;
            case TAB_MESSAGES:
                return new MessagesFragment();
            case TAB_PROFILE:
                return new ProfileFragment();
            default:
                return new DiscoverFragment();
        }
    }

    private void updateNav(boolean animate) {
        setNav(binding.navDiscover, binding.navDiscoverIcon, TAB_DISCOVER, animate);
        setNav(binding.navOrders, binding.navOrdersIcon, TAB_ORDERS, animate);
        setNav(binding.navMessages, binding.navMessagesIcon, TAB_MESSAGES, animate);
        setNav(binding.navProfile, binding.navProfileIcon, TAB_PROFILE, animate);
        if (TAB_MESSAGES.equals(current)) {
            binding.navBadge.setVisibility(View.GONE);
        }
    }

    private void setNav(View item, View icon, String tab, boolean animate) {
        boolean on = tab.equals(current);
        item.setSelected(on);
        icon.setSelected(on);
        icon.refreshDrawableState();
        if (on && animate) {
            Motion.bounce(icon);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TAB, current);
    }
}
