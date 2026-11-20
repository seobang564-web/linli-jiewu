package com.linlijiewu.app.data.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.linlijiewu.app.R;

/** 物品分类，与接口文档 Category 枚举一一对应。 */
public enum Category {
    TOOL(R.string.cat_tool, R.drawable.ic_tool, R.drawable.ill_tool, R.drawable.ic_pin_tool),
    LUGGAGE(R.string.cat_luggage, R.drawable.ic_luggage, R.drawable.ill_luggage, R.drawable.ic_pin_luggage),
    CAMPING(R.string.cat_camping, R.drawable.ic_camping, R.drawable.ill_camping, R.drawable.ic_pin_camping),
    SPORTS(R.string.cat_sports, R.drawable.ic_sports, R.drawable.ill_sports, R.drawable.ic_pin_sports),
    ELECTRONICS(R.string.cat_electronics, R.drawable.ic_electronics, R.drawable.ill_electronics, R.drawable.ic_pin_electronics),
    BOOK(R.string.cat_book, R.drawable.ic_book, R.drawable.ill_book, R.drawable.ic_pin_book),
    OTHER(R.string.cat_other, R.drawable.ic_other, R.drawable.ill_other, R.drawable.ic_pin_other);

    @StringRes public final int label;
    @DrawableRes public final int icon;
    @DrawableRes public final int illustration;
    @DrawableRes public final int pinIcon;

    Category(int label, int icon, int illustration, int pinIcon) {
        this.label = label;
        this.icon = icon;
        this.illustration = illustration;
        this.pinIcon = pinIcon;
    }
}
