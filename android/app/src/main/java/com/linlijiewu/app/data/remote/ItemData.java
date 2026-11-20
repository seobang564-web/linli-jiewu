package com.linlijiewu.app.data.remote;

import java.util.List;

public final class ItemData {
    public long id;
    public String title;
    public String category;
    public String coverUrl;
    public String status;
    public Integer distanceM;
    public String pickupLabel;
    public double pickupLat;
    public double pickupLng;
    public String description;
    public Owner owner;
    public String createdAt;
    public boolean isMine;

    public static final class Owner { public long id; public String nickname; }
    public static final class Page { public long total; public int page; public int size; public List<ItemData> list; }
    public static final class Uploaded { public String imageId; public String url; }
    public static final class Write {
        public String title;
        public String category;
        public String description;
        public String imageId;
        public double pickupLat;
        public double pickupLng;
        public String pickupLabel;
    }
}
