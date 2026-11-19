package com.linlijiewu.app.util;

import androidx.annotation.NonNull;
import com.linlijiewu.app.R;

/**
 * 物品分类枚举，与 docs/接口文档.md 的 Category 一一对应。
 */
public enum ItemCategory {

    TOOL("TOOL", R.string.category_tool),
    LUGGAGE("LUGGAGE", R.string.category_luggage),
    CAMPING("CAMPING", R.string.category_camping),
    SPORTS("SPORTS", R.string.category_sports),
    ELECTRONICS("ELECTRONICS", R.string.category_electronics),
    BOOK("BOOK", R.string.category_book),
    OTHER("OTHER", R.string.category_other);

    public final String apiValue;
    public final int labelRes;

    ItemCategory(String apiValue, int labelRes) {
        this.apiValue = apiValue;
        this.labelRes = labelRes;
    }

    @NonNull
    public static ItemCategory fromApiValue(@NonNull String value) {
        for (ItemCategory category : values()) {
            if (category.apiValue.equals(value)) {
                return category;
            }
        }
        return OTHER;
    }
}
