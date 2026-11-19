package com.linlijiewu.app.util;

import androidx.annotation.NonNull;
import com.linlijiewu.app.R;

/**
 * 物品状态展示：枚举 → 中文标签 + 全组统一状态色（colors.xml）。
 */
public final class ItemStatusStyle {

    private ItemStatusStyle() {
    }

    public static int labelRes(@NonNull String status) {
        switch (status) {
            case "RESERVED":
                return R.string.status_reserved;
            case "LENT":
                return R.string.status_lent;
            case "OFFLINE":
                return R.string.status_offline;
            case "AVAILABLE":
            default:
                return R.string.status_available;
        }
    }

    public static int colorRes(@NonNull String status) {
        switch (status) {
            case "RESERVED":
                return R.color.item_reserved;
            case "LENT":
                return R.color.item_lent;
            case "OFFLINE":
                return R.color.item_offline;
            case "AVAILABLE":
            default:
                return R.color.item_available;
        }
    }
}
