package com.linlijiewu.app.ui.borrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.FragmentBorrowBinding;

/**
 * 借还页（我借入的 / 我借出的）。第 1 周为空壳，订单列表与状态操作第 4 周接入。
 * 原型见 docs/页面原型.md 第 3 节。
 */
public class BorrowFragment extends Fragment {

    private FragmentBorrowBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBorrowBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.comingSoonDescription.setText(R.string.borrow_coming_soon);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
