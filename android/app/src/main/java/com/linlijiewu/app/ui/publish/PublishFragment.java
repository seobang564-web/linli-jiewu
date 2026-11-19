package com.linlijiewu.app.ui.publish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.FragmentPublishBinding;

/**
 * 发布页。第 1 周为空壳：占位布局与实现计划说明；表单与图片压缩在第 2 周填充。
 * 原型见 docs/页面原型.md 第 2 节。
 */
public class PublishFragment extends Fragment {

    private FragmentPublishBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPublishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.comingSoonDescription.setText(R.string.publish_coming_soon);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
