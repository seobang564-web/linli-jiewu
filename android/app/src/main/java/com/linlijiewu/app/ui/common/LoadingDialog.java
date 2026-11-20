package com.linlijiewu.app.ui.common;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import androidx.annotation.StringRes;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.DialogLoadingBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * 网络请求加载层：背景变暗，白色卡片里小邻抱着包裹小跑，文案随请求变化。
 * 正式接口接入后：请求开始 show()，回调里 dismiss()；原型阶段用 {@link #run} 模拟约 1 秒的请求。
 */
public final class LoadingDialog {
    private final Dialog dialog;
    private final DialogLoadingBinding binding;
    private final List<Animator> animators = new ArrayList<>();

    public LoadingDialog(Context context) {
        dialog = new Dialog(context, R.style.Linli_LoadingDialog);
        binding = DialogLoadingBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        dialog.setCancelable(false);
        dialog.setOnDismissListener(d -> {
            for (Animator a : animators) {
                a.cancel();
            }
            animators.clear();
        });
    }

    public void show(@StringRes int text) {
        binding.loadingText.setText(text);
        dialog.show();
        if (Motion.enabled(dialog.getContext())) {
            binding.loadingCard.startAnimation(AnimationUtils.loadAnimation(dialog.getContext(), R.anim.pop_in));
        }
        animators.addAll(Motion.mascotRun(binding.loadingMascot, binding.loadingShadow, binding.loadingSpeed));
        animators.addAll(Motion.dots(binding.loadingDots));
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /** 原型阶段：显示约 1 秒后执行 next。 */
    public static void run(Context context, @StringRes int text, Runnable next) {
        LoadingDialog loading = new LoadingDialog(context);
        loading.show(text);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loading.dismiss();
            next.run();
        }, 1000);
    }
}
