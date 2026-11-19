package com.linlijiewu.app.data.remote;

/**
 * 🔒 POST /items 发布请求体（接口文档第 5 节）。
 * title 1–50 字；description ≤500 字，可为 null；imageId 可为 null（无图发布）。
 */
public final class PublishItemRequest {

    private final String title;
    private final String category;
    private final String description;
    private final String imageId;
    private final Double pickupLat;
    private final Double pickupLng;
    private final String pickupLabel;

    public PublishItemRequest(String title, String category, String description,
                              String imageId, Double pickupLat, Double pickupLng, String pickupLabel) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.imageId = imageId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.pickupLabel = pickupLabel;
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

    public String getImageId() {
        return imageId;
    }

    public Double getPickupLat() {
        return pickupLat;
    }

    public Double getPickupLng() {
        return pickupLng;
    }

    public String getPickupLabel() {
        return pickupLabel;
    }
}
