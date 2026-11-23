package com.linlijiewu.app.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.BorrowStatus;
import com.linlijiewu.app.data.remote.*;
import com.linlijiewu.app.databinding.*;
import com.linlijiewu.app.ui.auth.AuthActivity;
import com.linlijiewu.app.ui.common.*;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;

public class OrdersFragment extends Fragment {
    private FragmentOrdersBinding binding;
    private boolean ownerView;
    private BorrowStatus filter;
    private Call<?> active;
    private int sequence, page;
    private long total;
    private boolean loading;
    private final List<BorrowData> orders = new ArrayList<>();

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false); return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.padTop(binding.tabBar);
        ownerView = state != null ? state.getBoolean("owner") : getArguments() != null && getArguments().getBoolean("owner");
        if (state != null && state.getString("filter") != null) { filter = BorrowStatus.valueOf(state.getString("filter")); }
        binding.tabBorrower.setOnClickListener(v -> setRole(false));
        binding.tabOwner.setOnClickListener(v -> setRole(true));
        renderFilters(); binding.tabBar.post(() -> { if (binding != null) { setRole(ownerView); } });
    }
    private void setRole(boolean owner) {
        if (binding == null) { return; } ownerView = owner;
        binding.tabBorrower.setSelected(!owner); binding.tabOwner.setSelected(owner);
        TextView target = owner ? binding.tabOwner : binding.tabBorrower;
        binding.tabIndicator.setX(target.getX() + (target.getWidth() - binding.tabIndicator.getWidth()) / 2f);
        load(true);
    }
    public void showRole(boolean owner) { ownerView = owner; if (binding != null) { setRole(owner); } }
    private void renderFilters() {
        binding.filterRow.removeAllViews();
        addFilter(null); for (BorrowStatus state : BorrowStatus.values()) { addFilter(state); }
    }
    private void addFilter(BorrowStatus status) {
        TextView chip = new TextView(requireContext(), null, 0, R.style.Chip);
        chip.setText(status == null ? R.string.orders_filter_all : status.label); chip.setSelected(status == filter);
        chip.setOnClickListener(v -> { filter = status; renderFilters(); load(true); });
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(-2, -2); lp.setMarginEnd(Ui.dp(requireContext(), 8));
        binding.filterRow.addView(chip, lp);
    }
    private void load(boolean reset) {
        if (binding == null) { return; }
        if (!reset && loading) { return; }
        if (active != null) { active.cancel(); }
        int query = reset ? ++sequence : sequence;
        if (!Session.isLoggedIn(requireContext())) {
            orders.clear(); total = 0; binding.list.removeAllViews();
            binding.list.addView(Ui.message(requireContext(), getString(R.string.orders_login),
                    () -> startActivity(new Intent(requireContext(), AuthActivity.class).putExtra("relogin", true))));
            return;
        }
        if (reset) { orders.clear(); page = 0; total = 0; }
        loading = true; render();
        var call = ApiClient.authenticated(requireContext()).borrows(ownerView ? "owner" : "borrower",
                filter == null ? null : filter.name(), page + 1, 20); active = call;
        call.enqueue(new ApiCallback<>(requireActivity(), data -> {
            if (binding == null || query != sequence) { return; }
            loading = false; page = data.page; total = data.total;
            if (data.list != null) { orders.addAll(data.list); } render();
        }, message -> {
            if (binding == null || query != sequence) { return; }
            loading = false; render();
            binding.list.addView(Ui.message(requireContext(), getString(R.string.nearby_retry, message), () -> load(reset)));
        }, () -> { }));
    }
    private void render() {
        binding.list.removeAllViews();
        if (loading) { binding.list.addView(Ui.message(requireContext(), getString(R.string.item_load), null)); }
        else if (orders.isEmpty()) { binding.list.addView(Ui.message(requireContext(), getString(R.string.orders_empty_real), () -> load(true))); }
        for (BorrowData order : orders) {
            ItemOrderBinding row = ItemOrderBinding.inflate(getLayoutInflater(), binding.list, false);
            BorrowData.Person peer = ownerView ? order.borrower : order.owner;
            row.peer.setText(getString(R.string.order_peer, getString(ownerView ? R.string.orders_borrower : R.string.orders_owner), peer.nickname));
            BorrowStatus status = BorrowStatus.valueOf(order.status); row.status.setText(status.label);
            row.meta.setText(getString(R.string.order_meta_real, order.id, Ui.date(order.dueAt)));
            Ui.thumb(row.thumb, order.item, 14, 0); row.title.setText(order.item.title);
            row.hint.setText(order.overdue ? getString(R.string.order_overdue) : order.item.pickupLabel);
            row.chat.setVisibility(View.GONE); row.secondary.setVisibility(View.GONE);
            row.primary.setText(BorrowActions.forUser(order, Session.userId(requireContext())).isEmpty() ? R.string.order_view : R.string.order_handle);
            View.OnClickListener open = v -> startActivity(OrderDetailActivity.intent(requireContext(), order.id));
            row.body.setOnClickListener(open); row.primary.setOnClickListener(open); binding.list.addView(row.getRoot());
        }
        if (!loading && orders.size() < total) { binding.list.addView(Ui.message(requireContext(), getString(R.string.load_more), () -> load(false))); }
    }
    @Override public void onResume() { super.onResume(); if (binding != null && !isHidden()) { load(true); } }
    @Override public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (!hidden) { load(true); } }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); out.putBoolean("owner", ownerView); if (filter != null) { out.putString("filter", filter.name()); }
    }
    @Override public void onDestroyView() {
        sequence++; if (active != null) { active.cancel(); } binding = null; super.onDestroyView();
    }
}
