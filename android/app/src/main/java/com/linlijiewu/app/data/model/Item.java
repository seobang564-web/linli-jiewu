package com.linlijiewu.app.data.model;

import androidx.annotation.ColorRes;

/** 物品（列表项 + 详情字段的精简版）。eastM / northM 是示例数据相对当前位置的偏移，用来在地图上摆放。 */
public final class Item {
    public final long id;
    public final String title;
    public final Category category;
    public final int distanceM;
    public final String pickupLabel;
    public final ItemStatus status;
    @ColorRes public final int tone;
    public final String description;
    public final String ownerName;
    public final double eastM;
    public final double northM;

    public Item(long id, String title, Category category, int distanceM, String pickupLabel, ItemStatus status,
                int tone, String description, String ownerName, double eastM, double northM) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.distanceM = distanceM;
        this.pickupLabel = pickupLabel;
        this.status = status;
        this.tone = tone;
        this.description = description;
        this.ownerName = ownerName;
        this.eastM = eastM;
        this.northM = northM;
    }
}
