package com.linlijiewu.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.FragmentProfileBinding;
import com.linlijiewu.app.databinding.ItemCategoryBinding;
import com.linlijiewu.app.ui.auth.AuthActivity;
import com.linlijiewu.app.ui.bootstrap.MainActivity;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Session;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.main.HomeActivity;
import com.linlijiewu.app.ui.item.MyItemsActivity;
import com.linlijiewu.app.data.remote.*;
import retrofit2.Call;

/** 个人中心（GET /users/me、GET /users/me/items）：三项数字概览、功能宫格、两张线稿卡片。 */
public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private Call<?> activeCall;
    private Call<?> borrowedCountCall, lentCountCall;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.padTop(binding.bannerText);
        boolean loggedIn = Session.isLoggedIn(requireContext());
        String nick = loggedIn ? Session.nickname(requireContext()) : getString(R.string.profile_guest);
        binding.avatar.setText(nick.isEmpty() ? "邻" : nick.substring(0, 1));
        binding.nickname.setText(nick);
        binding.meta.setText(loggedIn ? "@" + Session.username(requireContext()) + " · 楠苑" : getString(R.string.profile_guest_meta));
        binding.weekCount.setText(R.string.profile_real_session);
        binding.findItem.setOnClickListener(v -> MyItemsActivity.openId(requireContext()));
        binding.edit.setOnClickListener(v -> Ui.comingSoon(requireContext()));
        binding.statIn.setOnClickListener(v -> openOrders(false));
        binding.statOut.setOnClickListener(v -> openOrders(true));
        binding.statItems.setOnClickListener(v -> openMine());
        binding.guide.setOnClickListener(v -> Ui.comingSoon(requireContext()));
        binding.pact.setOnClickListener(v -> Ui.comingSoon(requireContext()));
        binding.logout.setOnClickListener(v -> {
            Session.logout(requireContext());
            startActivity(new Intent(requireContext(), AuthActivity.class).putExtra("relogin", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
        renderFeatures();
        animateIn();
    }

    private void renderFeatures() {
        int[][] features = {
                {R.drawable.ic_other, R.string.profile_f_items}, {R.drawable.ic_draft, R.string.profile_f_draft},
                {R.drawable.ic_in, R.string.profile_f_in}, {R.drawable.ic_out, R.string.profile_f_out},
                {R.drawable.ic_card, R.string.profile_f_contact}, {R.drawable.ic_history, R.string.profile_f_history},
                {R.drawable.ic_gps, R.string.profile_f_location}, {R.drawable.ic_shield, R.string.profile_f_server}};
        LayoutInflater inflater = getLayoutInflater();
        for (int[] f : features) {
            ItemCategoryBinding cell = ItemCategoryBinding.inflate(inflater, binding.features, false);
            cell.icon.setImageResource(f[0]);
            cell.label.setText(f[1]);
            cell.label.setTextSize(13);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.bottomMargin = Ui.dp(requireContext(), 14);
            cell.getRoot().setLayoutParams(lp);
            final int label = f[1];
            cell.getRoot().setOnClickListener(v -> {
                if (label == R.string.profile_f_in || label == R.string.profile_f_out) {
                    openOrders(label == R.string.profile_f_out);
                } else if (label == R.string.profile_f_server) {
                    startActivity(new Intent(requireContext(), MainActivity.class));
                } else if (label == R.string.profile_f_items) {
                    openMine();
                } else {
                    Ui.comingSoon(requireContext());
                }
            });
            binding.features.addView(cell.getRoot());
        }
    }

    private void openOrders(boolean owner) {
        startActivity(HomeActivity.intent(requireContext(), HomeActivity.TAB_ORDERS).putExtra("orders_owner", owner));
    }

    private void openMine() {
        startActivity(Session.isLoggedIn(requireContext()) ? new Intent(requireContext(), MyItemsActivity.class)
                : new Intent(requireContext(), AuthActivity.class).putExtra("relogin", true));
    }

    @Override public void onResume() { super.onResume(); refreshAccount(); }

    private void refreshAccount() {
        if (binding == null) { return; }
        boolean loggedIn = Session.isLoggedIn(requireContext());
        String nick = loggedIn ? Session.nickname(requireContext()) : getString(R.string.profile_guest);
        binding.nickname.setText(nick);
        binding.avatar.setText(nick.isEmpty() ? "" : nick.substring(0, 1));
        binding.meta.setText(loggedIn ? "@" + Session.username(requireContext()) : getString(R.string.profile_guest_meta));
        binding.statItemsValue.setText("0");
        binding.statInValue.setText(R.string.stats_pending); binding.statOutValue.setText(R.string.stats_pending);
        if (activeCall != null) { activeCall.cancel(); }
        if (borrowedCountCall != null) { borrowedCountCall.cancel(); }
        if (lentCountCall != null) { lentCountCall.cancel(); }
        if (loggedIn) {
            var borrowed = ApiClient.authenticated(requireContext()).borrows("borrower", null, 1, 1); borrowedCountCall = borrowed;
            borrowed.enqueue(new ApiCallback<>(requireActivity(), data -> { if (binding != null) { binding.statInValue.setText(Long.toString(data.total)); } }, m -> { }, () -> { }));
            var lent = ApiClient.authenticated(requireContext()).borrows("owner", null, 1, 1); lentCountCall = lent;
            lent.enqueue(new ApiCallback<>(requireActivity(), data -> { if (binding != null) { binding.statOutValue.setText(Long.toString(data.total)); } }, m -> { }, () -> { }));
            var call = ApiClient.authenticated(requireContext()).mine(1, 1); activeCall = call;
            call.enqueue(new ApiCallback<>(requireActivity(), data -> {
                if (binding != null) { binding.statItemsValue.setText(Long.toString(data.total)); }
            }, message -> {
                if (binding != null) {
                    binding.statItemsValue.setText(R.string.stats_pending);
                    if (!Session.isLoggedIn(requireContext())) { binding.nickname.setText(R.string.profile_guest); }
                }
            }, () -> { }));
        }
    }

    private void animateIn() {
        Motion.stagger(binding.content, 60);
        if (Motion.enabled(requireContext())) {
            binding.bannerArt.setTranslationY(Ui.dp(requireContext(), 24));
            binding.bannerArt.setAlpha(0f);
            binding.bannerArt.animate().translationY(0).alpha(1f).setStartDelay(150).setDuration(800)
                    .setInterpolator(Motion.easeOut(requireContext())).start();
        }
        // 下拉时头图轻微放大，上滑时视差
        binding.scroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, x, y, ox, oy) -> binding.bannerArt.setTranslationY(y * 0.3f));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            refreshAccount();
        }
    }

    @Override
    public void onDestroyView() {
        if (activeCall != null) { activeCall.cancel(); }
        if (borrowedCountCall != null) { borrowedCountCall.cancel(); }
        if (lentCountCall != null) { lentCountCall.cancel(); }
        binding = null;
        super.onDestroyView();
    }
}
