package com.linlijiewu.server.item;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.linlijiewu.server.common.BusinessException;

@Service
public class ItemSearchService {
    private static final double EARTH_RADIUS = 6371008.8;
    private final ItemMapper items;
    public ItemSearchService(ItemMapper items) { this.items = items; }

    @Transactional(readOnly = true)
    public ItemModels.Page<Result> search(double lat, double lng, int radius, String keyword, String category,
                                         String status, int page, int size) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180
                || radius < 100 || radius > 10000 || page < 1 || size < 1 || size > 50) {
            throw BusinessException.badRequest("坐标、半径或分页参数不正确");
        }
        List<String> statuses;
        try {
            statuses = status == null ? List.of("AVAILABLE") : Arrays.stream(status.split(",", -1))
                    .map(String::trim).map(value -> ItemModels.Status.valueOf(value).name()).distinct().toList();
            if (statuses.contains("OFFLINE")) { throw new IllegalArgumentException(); }
            if (category != null) { category = ItemModels.Category.valueOf(category).name(); }
        } catch (IllegalArgumentException exception) { throw BusinessException.badRequest("分类或物品状态不正确"); }
        keyword = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
        double angular = radius / EARTH_RADIUS;
        double deltaLat = Math.toDegrees(angular);
        double minLat = Math.max(-90, lat - deltaLat), maxLat = Math.min(90, lat + deltaLat);
        boolean allLongitudes = minLat <= -90 || maxLat >= 90;
        double deltaLng = allLongitudes ? 180 : Math.toDegrees(Math.asin(Math.min(1, Math.sin(angular) / Math.cos(Math.toRadians(lat)))));
        double minLng = lng - deltaLng, maxLng = lng + deltaLng;
        boolean wrap = minLng < -180 || maxLng > 180;
        if (minLng < -180) { minLng += 360; }
        if (maxLng > 180) { maxLng -= 360; }
        var candidates = items.nearby(statuses, category, keyword, minLat, maxLat, minLng, maxLng, wrap, allLongitudes);
        var results = candidates.stream().map(row -> new Measured(row, distance(lat, lng, row.pickupLat, row.pickupLng)))
                .filter(value -> value.meters <= radius)
                .map(value -> new Result(value.row.id, value.row.title, value.row.category, value.row.imageUrl,
                        value.row.status, (int) Math.round(value.meters), value.row.pickupLabel,
                        value.row.pickupLat, value.row.pickupLng))
                .sorted(Comparator.comparingInt(Result::distanceM).thenComparingLong(Result::id)).toList();
        long offset = (long) (page - 1) * size;
        var list = results.stream().skip(offset).limit(size).toList();
        return new ItemModels.Page<>(results.size(), page, size, list);
    }

    private static double distance(double lat, double lng, double otherLat, double otherLng) {
        double a = Math.pow(Math.sin(Math.toRadians(otherLat - lat) / 2), 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(otherLat))
                * Math.pow(Math.sin(Math.toRadians(otherLng - lng) / 2), 2);
        return 2 * EARTH_RADIUS * Math.asin(Math.sqrt(Math.min(1, a)));
    }
    private record Measured(ItemRow row, double meters) { }
    public record Result(long id, String title, String category, String coverUrl, String status, int distanceM,
                         String pickupLabel, double pickupLat, double pickupLng) { }
}
