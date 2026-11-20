package com.linlijiewu.app.ui.common;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import com.linlijiewu.app.R;
import java.util.ArrayList;
import java.util.List;

/**
 * 动效：与原型保持同一套节奏。出场用 expo-out，弹性用轻微回弹；时长 0.3–0.6 秒。
 * 系统关闭动画（开发者选项或无障碍“移除动画”）时一律直接显示最终状态。
 */
public final class Motion {
    private Motion() {
    }

    public static boolean enabled(Context c) {
        return Settings.Global.getFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    public static Interpolator easeOut(Context c) {
        return AnimationUtils.loadInterpolator(c, R.interpolator.ease_out_expo);
    }

    public static Interpolator spring(Context c) {
        return AnimationUtils.loadInterpolator(c, R.interpolator.spring_soft);
    }

    /** 容器里的直接子 View 依次上浮出现。 */
    public static void stagger(ViewGroup group, long startDelay) {
        if (group == null || !enabled(group.getContext())) {
            return;
        }
        float dy = Ui.dp(group.getContext(), 18);
        int shown = 0;
        for (int i = 0; i < group.getChildCount() && shown < 14; i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            child.setAlpha(0f);
            child.setTranslationY(dy);
            child.animate().alpha(1f).translationY(0f).setStartDelay(startDelay + shown * 45L)
                    .setDuration(520).setInterpolator(easeOut(group.getContext())).start();
            shown++;
        }
    }

    /** 从侧边滑入（切换借入/借出时用）。 */
    public static void slideIn(ViewGroup group, int direction) {
        if (group == null || !enabled(group.getContext())) {
            return;
        }
        float dx = Ui.dp(group.getContext(), 40) * direction;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationX(dx);
            child.animate().alpha(1f).translationX(0f).setStartDelay(i * 45L).setDuration(480)
                    .setInterpolator(easeOut(group.getContext())).start();
        }
    }

    public static void popIn(View view, long delay) {
        if (view == null || !enabled(view.getContext())) {
            return;
        }
        view.setAlpha(0f);
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(420)
                .setInterpolator(spring(view.getContext())).start();
    }

    /** 选中时轻弹一下（导航图标、筛选标签）。 */
    public static void bounce(View view) {
        if (view == null || !enabled(view.getContext())) {
            return;
        }
        view.setScaleX(0.86f);
        view.setScaleY(0.86f);
        view.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(420)
                .setInterpolator(spring(view.getContext())).start();
    }

    /** 数字从 0 滚动到目标值。 */
    public static void countUp(TextView view, int target, long delay) {
        if (!enabled(view.getContext()) || target <= 0) {
            view.setText(String.valueOf(target));
            return;
        }
        view.setText("0");
        ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setStartDelay(delay);
        a.setDuration(900);
        a.setInterpolator(easeOut(view.getContext()));
        a.addUpdateListener(v -> view.setText(String.valueOf(v.getAnimatedValue())));
        a.start();
    }

    /** 小邻抱着包裹一路小跑：上下蹦、落地压扁、影子跟着缩放、速度线往后飘。 */
    public static List<Animator> mascotRun(View mascot, View shadow, View speed) {
        List<Animator> list = new ArrayList<>();
        if (!enabled(mascot.getContext())) {
            return list;
        }
        float lift = -Ui.dp(mascot.getContext(), 7);
        mascot.setPivotY(Ui.dp(mascot.getContext(), 86));
        list.add(loop(ObjectAnimator.ofFloat(mascot, View.TRANSLATION_Y, 0f, lift), 250));
        list.add(loop(ObjectAnimator.ofFloat(mascot, View.SCALE_X, 1.04f, 0.98f), 250));
        list.add(loop(ObjectAnimator.ofFloat(mascot, View.SCALE_Y, 0.96f, 1.03f), 250));
        list.add(loop(ObjectAnimator.ofFloat(shadow, View.SCALE_X, 1f, 0.7f), 250));
        list.add(loop(ObjectAnimator.ofFloat(shadow, View.ALPHA, 1f, 0.6f), 250));
        ObjectAnimator drift = ObjectAnimator.ofFloat(speed, View.TRANSLATION_X, Ui.dp(mascot.getContext(), 4), -Ui.dp(mascot.getContext(), 8));
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.setDuration(600);
        ObjectAnimator fade = ObjectAnimator.ofFloat(speed, View.ALPHA, 0f, 0.9f, 0f);
        fade.setRepeatCount(ValueAnimator.INFINITE);
        fade.setDuration(600);
        list.add(drift);
        list.add(fade);
        for (Animator a : list) {
            a.start();
        }
        return list;
    }

    /** 三个小绿点依次闪烁。 */
    public static List<Animator> dots(ViewGroup dots) {
        List<Animator> list = new ArrayList<>();
        if (!enabled(dots.getContext())) {
            return list;
        }
        for (int i = 0; i < dots.getChildCount(); i++) {
            ObjectAnimator a = ObjectAnimator.ofFloat(dots.getChildAt(i), View.ALPHA, 0.25f, 1f, 0.25f);
            a.setDuration(1100);
            a.setStartDelay(i * 180L);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.start();
            list.add(a);
        }
        return list;
    }

    /** 轻轻上下浮动（空状态等静态表情）。 */
    public static Animator bob(View view) {
        if (!enabled(view.getContext())) {
            return new AnimatorSet();
        }
        ObjectAnimator a = loop(ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -Ui.dp(view.getContext(), 4)), 600);
        a.start();
        return a;
    }

    private static ObjectAnimator loop(ObjectAnimator a, long duration) {
        a.setDuration(duration);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        return a;
    }
}
