package com.linlijiewu.server.item;

import java.time.LocalDateTime;

public class ItemRow {
    public Long id;
    public Long ownerId;
    public String title;
    public String category;
    public String description;
    public String imageUrl;
    public Double pickupLat;
    public Double pickupLng;
    public String pickupLabel;
    public String status;
    public Long activeBorrowId;
    public LocalDateTime createdAt;
    public String ownerNickname;
}
