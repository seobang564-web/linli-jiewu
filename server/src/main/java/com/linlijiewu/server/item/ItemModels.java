package com.linlijiewu.server.item;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.constraints.*;

public final class ItemModels {
    private ItemModels() { }
    public enum Category { TOOL, LUGGAGE, CAMPING, SPORTS, ELECTRONICS, BOOK, OTHER }
    public enum Status { AVAILABLE, RESERVED, LENT, OFFLINE }
    public record Write(
            @NotBlank @Size(max = 50) String title,
            @NotNull Category category,
            @Size(max = 500) String description,
            @Size(max = 64) String imageId,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double pickupLat,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double pickupLng,
            @NotBlank @Size(max = 100) String pickupLabel) { }
    public record Owner(long id, String nickname) { }
    public record Detail(long id, String title, String category, String coverUrl,
                         String status, Integer distanceM, String pickupLabel, double pickupLat,
                         double pickupLng, String description, Owner owner,
                         OffsetDateTime createdAt, Boolean isMine) { }
    public record Page<T>(long total, int page, int size, List<T> list) { }
}
