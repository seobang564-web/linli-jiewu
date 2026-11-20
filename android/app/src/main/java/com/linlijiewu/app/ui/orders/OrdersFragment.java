package com.linlijiewu.app.ui.orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.BorrowStatus;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.model.Order;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.FragmentOrdersBinding;
import com.linlijiewu.app.databinding.ItemOrderBinding;
import com.linlijiewu.app.ui.chat.ChatActivity;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import java.util.List;

/** 订单：我借入的 / 我借出的（GET /borrows?role=borrower|owner）。按钮随状态机变化。 */
public class OrdersFragment extends Fragment {
    private static final BorrowStatus[] FILTERS = {null, BorrowStatus.PENDING, BorrowStatus.ACCEPTED,
            BorrowStatus.BORROWING, BorrowStatus.RETURNING, BorrowStatus.COMPLETED};

    private FragmentOrdersBinding binding;
    private boolean ownerView;
    private BorrowStatus filter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.padTop(binding.tabBar);
        binding.tabBorrower.setOnClickListener(v -> setRole(false, true));
        binding.tabOwner.setOnClickListener(v -> setRole(true, true));
        renderFilters();
        binding.tabBar.post(() -> setRole(false, false));
        Motion.stagger(binding.list, 120);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            Motion.stagger(binding.list, 60);
        }
    }

    private void setRole(boolean owner, boolean animate) {
        boolean changed = owner != ownerView;
        ownerView = owner;
        binding.tabBorrower.setSelected(!owner);
        binding.tabOwner.setSelected(owner);
        binding.tabBorrower.setTypeface(null, owner ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        binding.tabOwner.setTypeface(null, owner ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        TextView target = owner ? binding.tabOwner : binding.tabBorrower;
        float x = target.getX() + (target.getWidth() - binding.tabIndicator.getWidth()) / 2f;
        if (animate && Motion.enabled(requireContext())) {
            binding.tabIndicator.animate().x(x).setDuration(420).setInterpolator(Motion.easeOut(requireContext())).start();
        } else {
            binding.tabIndicator.setX(x);
        }
        renderList();
        if (animate && changed) {
            Motion.slideIn(binding.list, owner ? 1 : -1);
        }
    }

    private void renderFilters() {
        binding.filterRow.removeAllViews();
        for (BorrowStatus s : FILTERS) {
            TextView chip = new TextView(requireContext(), null, 0, R.style.Chip);
            chip.setText(s == null ? getString(R.string.orders_filter_all) : getString(s.label));
            chip.setSelected(s == filter);
            chip.setOnClickListener(v -> {
                filter = s;
                renderFilters();
                renderList();
                Motion.stagger(binding.list, 0);
            });
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(requireContext(), 8));
            binding.filterRow.addView(chip, lp);
        }
    }

    private void renderList() {
        binding.list.removeAllViews();
        List<Order> orders = ownerView ? SampleData.OWNER_ORDERS : SampleData.BORROWER_ORDERS;
        LayoutInflater inflater = getLayoutInflater();
        for (Order o : orders) {
            if (filter != null && o.status != filter) {
                continue;
            }
            binding.list.addView(bind(inflater, o));
        }
    }

    private View bind(LayoutInflater inflater, Order o) {
        ItemOrderBinding row = ItemOrderBinding.inflate(inflater, binding.list, false);
        Item item = SampleData.item(o.itemId);
        if (o.status.isActive()) {
            row.card.setBackgroundResource(R.drawable.bg_contour);
        }
        row.peer.setText(o.peerLabel);
        row.status.setText(getString(o.status.label) + " ›");
        int statusColor = o.status == BorrowStatus.PENDING ? R.color.amber
                : o.status == BorrowStatus.CANCELLED ? R.color.sub : R.color.ink;
        row.status.setTextColor(ContextCompat.getColor(requireContext(), statusColor));
        row.meta.setText(o.meta);
        Ui.thumb(row.thumb, item, 14, 0.2f);
        row.title.setText(item.title);
        row.hint.setText(o.hint);
        row.hint.setTextColor(ContextCompat.getColor(requireContext(), o.warn ? R.color.coral : R.color.sub));
        bindAction(row.secondary, o.secondaryAction);
        bindAction(row.primary, o.primaryAction);
        row.actions.setVisibility(o.secondaryAction == null && o.primaryAction == null ? View.GONE : View.VISIBLE);
        row.chat.setOnClickListener(v -> startActivity(ChatActivity.intent(requireContext(), item.id)));
        row.body.setOnClickListener(v -> startActivity(OrderDetailActivity.intent(requireContext(), o.id)));
        return row.getRoot();
    }

    private void bindAction(TextView button, @Nullable String label) {
        if (label == null) {
            button.setVisibility(View.GONE);
            return;
        }
        button.setText(label);
        button.setOnClickListener(v -> Ui.comingSoon(requireContext()));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
