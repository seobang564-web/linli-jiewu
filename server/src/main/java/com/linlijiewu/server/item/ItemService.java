package com.linlijiewu.server.item;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.linlijiewu.server.common.BusinessException;
import com.linlijiewu.server.image.ImageService;

@Service
public class ItemService {
    private final ItemMapper items;
    private final ImageService images;
    public ItemService(ItemMapper items, ImageService images) { this.items = items; this.images = images; }

    @Transactional
    public ItemModels.Detail create(long ownerId, ItemModels.Write request) {
        ItemRow item = new ItemRow();
        item.ownerId = ownerId;
        assign(item, request);
        item.imageUrl = images.resolveOwned(request.imageId(), ownerId);
        items.insert(item);
        return detail(item.id, ownerId, null, null);
    }

    public ItemModels.Detail detail(long id, Long viewer, Double lat, Double lng) {
        validateCoordinates(lat, lng);
        ItemRow item = items.find(id);
        if (item == null || ("OFFLINE".equals(item.status) && !Objects.equals(item.ownerId, viewer))) {
            throw BusinessException.notFound();
        }
        return view(item, viewer, lat, lng);
    }

    @Transactional
    public ItemModels.Detail update(long id, long ownerId, ItemModels.Write request) {
        ItemRow item = ownLocked(id, ownerId);
        if ("OFFLINE".equals(item.status)) { throw BusinessException.conflict("已下架物品不可编辑"); }
        boolean changedPickup = Double.compare(item.pickupLat, request.pickupLat()) != 0
                || Double.compare(item.pickupLng, request.pickupLng()) != 0
                || !item.pickupLabel.equals(request.pickupLabel().trim());
        if (hasActiveOrder(item) && changedPickup) {
            throw BusinessException.conflict("存在进行中订单，不可修改交接点");
        }
        assign(item, request);
        item.imageUrl = images.resolveOwned(request.imageId(), ownerId);
        items.update(item);
        return detail(id, ownerId, null, null);
    }

    @Transactional
    public ItemModels.Detail offline(long id, long ownerId) {
        ItemRow item = ownLocked(id, ownerId);
        if (hasActiveOrder(item)) { throw BusinessException.conflict("存在进行中订单，不可下架"); }
        items.offline(id);
        return detail(id, ownerId, null, null);
    }

    @Transactional(readOnly = true)
    public ItemModels.Page<ItemModels.Detail> mine(long ownerId, String status, int page, int size) {
        if (page < 1 || size < 1 || size > 50) { throw BusinessException.badRequest("page 必须从 1 开始，size 为 1–50"); }
        List<String> statuses = null;
        if (status != null) {
            try {
                statuses = Arrays.stream(status.split(",", -1)).map(String::trim)
                        .map(value -> ItemModels.Status.valueOf(value).name()).distinct().toList();
            } catch (IllegalArgumentException exception) { throw BusinessException.badRequest("物品状态不正确"); }
        }
        long total = items.countMine(ownerId, statuses);
        var list = items.mine(ownerId, statuses, size, (long) (page - 1) * size).stream()
                .map(item -> view(item, ownerId, null, null)).toList();
        return new ItemModels.Page<>(total, page, size, list);
    }

    private ItemRow ownLocked(long id, long ownerId) {
        ItemRow item = items.lock(id);
        if (item == null) { throw BusinessException.notFound(); }
        if (item.ownerId != ownerId) { throw BusinessException.forbidden(); }
        return item;
    }

    private boolean hasActiveOrder(ItemRow item) {
        return item.activeBorrowId != null || "RESERVED".equals(item.status) || "LENT".equals(item.status);
    }

    private void assign(ItemRow item, ItemModels.Write request) {
        if (!Double.isFinite(request.pickupLat()) || !Double.isFinite(request.pickupLng())) {
            throw BusinessException.badRequest("坐标不正确");
        }
        item.title = request.title().trim();
        item.category = request.category().name();
        item.description = request.description() == null ? "" : request.description();
        item.pickupLat = request.pickupLat();
        item.pickupLng = request.pickupLng();
        item.pickupLabel = request.pickupLabel().trim();
    }

    private void validateCoordinates(Double lat, Double lng) {
        if ((lat == null) != (lng == null) || (lat != null && (!Double.isFinite(lat) || !Double.isFinite(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180))) {
            throw BusinessException.badRequest("lat、lng 须同时提供且位于合法范围");
        }
    }

    private ItemModels.Detail view(ItemRow item, Long viewer, Double lat, Double lng) {
        Integer distance = null;
        if (lat != null) {
            double deltaLat = Math.toRadians(item.pickupLat - lat);
            double deltaLng = Math.toRadians(item.pickupLng - lng);
            double a = Math.pow(Math.sin(deltaLat / 2), 2) + Math.cos(Math.toRadians(lat))
                    * Math.cos(Math.toRadians(item.pickupLat)) * Math.pow(Math.sin(deltaLng / 2), 2);
            distance = (int) Math.round(2 * 6371008.8 * Math.asin(Math.sqrt(Math.min(1, a))));
        }
        return new ItemModels.Detail(item.id, item.title, item.category, item.imageUrl, item.status,
                distance, item.pickupLabel, item.pickupLat, item.pickupLng, item.description,
                new ItemModels.Owner(item.ownerId, item.ownerNickname),
                item.createdAt.atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime(),
                viewer == null ? null : Objects.equals(viewer, item.ownerId));
    }
}
