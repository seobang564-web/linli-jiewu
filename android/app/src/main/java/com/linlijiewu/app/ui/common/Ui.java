package com.linlijiewu.app.ui.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.model.ItemStatus;

/** 各页面共用的小工具：尺寸换算、缩略图、状态胶囊、距离文字、系统栏留白。 */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** 物品缩略图：浅色底 + 线稿插画。 */
    public static void thumb(ImageView view, Item item, float radiusDp, float paddingRatio) {
        Context c = view.getContext();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(c, item.tone));
        bg.setCornerRadius(dp(c, radiusDp));
        view.setBackground(bg);
        view.setImageResource(item.category.illustration);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.post(() -> {
            int pad = Math.round(view.getWidth() * paddingRatio);
            view.setPadding(pad, pad, pad, pad);
        });
    }

    public static void statusPill(TextView view, ItemStatus status) {
        view.setText(status.label);
        view.setBackgroundResource(status.background);
        view.setTextColor(ContextCompat.getColor(view.getContext(), status.textColor));
    }

    /** 纯文本距离：“距离 120m” / “距离 1.2km”。 */
    public static String distanceText(Context c, int meters) {
        String value = meters < 1000 ? c.getString(R.string.distance_m, meters) : c.getString(R.string.distance_km, meters / 1000f);
        return c.getString(R.string.pickup_distance_text, value);
    }

    /** “486m” 这类距离：数字大、单位小。 */
    public static CharSequence distance(Context c, int meters, int numberSp) {
        String number = meters < 1000 ? String.valueOf(meters) : String.format(java.util.Locale.ROOT, "%.1f", meters / 1000f);
        String unit = meters < 1000 ? "m" : "km";
        SpannableString s = new SpannableString(number + unit);
        s.setSpan(new AbsoluteSizeSpan(numberSp, true), 0, number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new TypefaceSpan("sans-serif-medium"), 0, number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new AbsoluteSizeSpan(12, true), number.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    public static TextView tag(Context c, CharSequence text) {
        TextView tv = new TextView(c, null, 0, R.style.T_Tag);
        tv.setText(text);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(c, 6));
        tv.setLayoutParams(lp);
        return tv;
    }

    public static void comingSoon(Context c) {
        Toast.makeText(c, R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }

    /** 顶部留出状态栏高度（叠加原有 padding）。 */
    public static void padTop(View view) {
        final int base = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), base + bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /** 底部留出导航条高度（叠加原有 padding）。 */
    public static void padBottom(View view) {
        final int base = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), base + bars.bottom);
            return insets;
        });
    }

    public static void back(androidx.activity.ComponentActivity activity, View button) {
        button.setOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());
    }

    public static void thumb(ImageView view, com.linlijiewu.app.data.remote.ItemData item, float radiusDp, float paddingRatio) {
        Context c = view.getContext();
        com.linlijiewu.app.data.model.Category category = category(item.category);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(c, com.linlijiewu.app.R.color.tone_olive));
        bg.setCornerRadius(dp(c, radiusDp)); view.setBackground(bg); view.setClipToOutline(true); view.setPadding(0, 0, 0, 0);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        com.bumptech.glide.Glide.with(view).load(com.linlijiewu.app.data.remote.FileUrls.resolve(
                com.linlijiewu.app.BuildConfig.SERVER_BASE_URL, item.coverUrl))
                .placeholder(category.illustration).error(category.illustration).into(view);
    }

    public static com.linlijiewu.app.data.model.Category category(String value) {
        try { return com.linlijiewu.app.data.model.Category.valueOf(value); }
        catch (IllegalArgumentException | NullPointerException exception) { return com.linlijiewu.app.data.model.Category.OTHER; }
    }

    public static String date(String value) { return value == null ? "" : value.replace('T', ' ').substring(0, Math.min(16, value.length())); }

    public static int borrowActionLabel(String action) {
        switch (action) {
            case "accept": return R.string.action_accept;
            case "reject": return R.string.action_reject;
            case "cancel": return R.string.action_cancel;
            case "handover": return R.string.action_handover;
            case "return": return R.string.action_return;
            case "confirm-return": return R.string.action_confirm_return;
            case "deny-return": return R.string.action_deny_return;
            default: return R.string.order_view;
        }
    }

    public static TextView message(Context c, CharSequence message, Runnable retry) {
        TextView text = new TextView(c, null, 0, R.style.T_Body);
        text.setText(message); int pad = dp(c, 18); text.setPadding(pad, pad, pad, pad);
        if (retry != null) { text.setOnClickListener(v -> retry.run()); }
        return text;
    }
}
