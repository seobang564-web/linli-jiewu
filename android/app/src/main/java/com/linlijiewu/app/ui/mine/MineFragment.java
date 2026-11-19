package com.linlijiewu.app.ui.mine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.FragmentMineBinding;
import com.linlijiewu.app.ui.bootstrap.MainActivity;

/**
 * 我的页。第 1 周为空壳：保留服务连接自检入口（bootstrap 页），
 * 我的发布与本地草稿第 3 周接入。原型见 docs/页面原型.md 第 4 节。
 */
public class MineFragment extends Fragment {

    private FragmentMineBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.comingSoonDescription.setText(R.string.mine_coming_soon);
        binding.selfCheckEntry.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MainActivity.class)));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
