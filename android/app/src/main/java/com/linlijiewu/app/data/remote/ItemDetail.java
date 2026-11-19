package com.linlijiewu.app.data.remote;

/**
 * GET /items/{id} 与 POST /items 的物品详情，字段与 docs/接口文档.md 第 5 节一致。
 */
public final class ItemDetail {
    private long id;
    private String title;
    private String category;
    private String description;
    private String coverUrl;
    private String status;
    private Integer distanceM;
    private String pickupLabel;
    private double pickupLat;
    private double pickupLng;
    private ItemOwner owner;
    private String createdAt;
    private Boolean isMine;

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getStatus() {
        return status;
    }

    public Integer getDistanceM() {
        return distanceM;
    }

    public String getPickupLabel() {
        return pickupLabel;
    }

    public double getPickupLat() {
        return pickupLat;
    }

    public double getPickupLng() {
        return pickupLng;
    }

    public ItemOwner getOwner() {
        return owner;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Boolean getIsMine() {
        return isMine;
    }
}
