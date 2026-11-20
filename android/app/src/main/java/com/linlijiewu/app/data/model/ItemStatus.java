package com.linlijiewu.app.data.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.linlijiewu.app.R;

/** 物品状态，与接口文档 ItemStatus 对应；颜色只在这里定义一次。 */
public enum ItemStatus {
    AVAILABLE(R.string.status_available, R.drawable.bg_pill_lime, R.color.item_available),
    RESERVED(R.string.status_reserved, R.drawable.bg_pill_amber, R.color.item_reserved),
    LENT(R.string.status_lent, R.drawable.bg_pill_gray, R.color.item_lent),
    OFFLINE(R.string.status_offline, R.drawable.bg_pill_gray, R.color.item_offline);

    @StringRes public final int label;
    @DrawableRes public final int background;
    @ColorRes public final int textColor;

    ItemStatus(int label, int background, int textColor) {
        this.label = label;
        this.background = background;
        this.textColor = textColor;
    }
}
