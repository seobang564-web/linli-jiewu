package com.linlijiewu.server;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemSearchIntegrationTest extends BorrowTestSupport {
    private long publish(Account owner, String title, double lat, double lng, String category) throws Exception {
        var body = itemBody(); body.put("title", title); body.put("pickupLat", lat); body.put("pickupLng", lng); body.put("category", category);
        return data(send("/api/items", body, owner)).path("id").asLong();
    }

    @Test void rawDistanceFiltersBeforeRoundingAt499500501Meters() throws Exception {
        Account owner = account(); String tag = key().substring(0, 8);
        long near = publish(owner, "【测试】" + tag, 0.0044876, 0, "TOOL");
        long edge = publish(owner, "【测试】" + tag, 0.0044966, 0, "TOOL");
        publish(owner, "【测试】" + tag, 0.0044967, 0, "TOOL"); // 500.011 m rounds to 500 but is outside.
        publish(owner, "【测试】" + tag, 0.0045056, 0, "TOOL");
        read("/api/items?lat=0&lng=0&radius=500&keyword=" + tag, null)
                .andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.list[0].id").value(near))
                .andExpect(jsonPath("$.data.list[1].id").value(edge)).andExpect(jsonPath("$.data.list[1].distanceM").value(500))
                .andExpect(jsonPath("$.data.list[0].owner").doesNotExist());
    }

    @Test void literalSubstringCategoryAndStatusFiltersUseRealRows() throws Exception {
        Account owner = account(), borrower = account(); String tag = key().substring(0, 8);
        long a = publish(owner, tag + " Camp 100%_", 0, 0, "CAMPING");
        long b = publish(owner, tag + " CAMP plain", 0, 0, "TOOL");
        long c = publish(owner, tag + " camping", 0, 0, "CAMPING");
        borrow(c, borrower);
        mvc.perform(get("/api/items").param("lat", "0").param("lng", "0").param("keyword", tag + " cAmP").param("category", "CAMPING"))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.list[0].id").value(a));
        mvc.perform(get("/api/items").param("lat", "0").param("lng", "0").param("keyword", "100%_"))
                .andExpect(jsonPath("$.data.total").value(1));
        read("/api/items?lat=0&lng=0&keyword=" + tag + "&status=AVAILABLE,RESERVED,AVAILABLE", null)
                .andExpect(jsonPath("$.data.total").value(3));
        send("/api/items/" + b + "/offline", java.util.Map.of(), owner).andExpect(status().isOk());
        read("/api/items?lat=0&lng=0&keyword=" + tag, null).andExpect(jsonPath("$.data.total").value(1));
    }

    @Test void longitudeWrapAndPolarQueriesRetainNearbyItems() throws Exception {
        Account owner = account(); String tag = key().substring(0, 8);
        long across = publish(owner, tag, 0, -179.999, "OTHER");
        publish(owner, tag, 0, 179.98, "OTHER");
        read("/api/items?lat=0&lng=179.999&radius=500&keyword=" + tag, null)
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.list[0].id").value(across));
        long polar = publish(owner, tag, 89.999, 180, "OTHER");
        read("/api/items?lat=90&lng=0&radius=500&keyword=" + tag, null)
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.list[0].id").value(polar));
    }

    @Test void roundedDistanceTiesSortByIdAndPaginationIsStable() throws Exception {
        Account owner = account(); String tag = key().substring(0, 8);
        long first = publish(owner, tag, 0, 0, "BOOK"), second = publish(owner, tag, 0, 0, "BOOK");
        publish(owner, tag, 0, 0, "BOOK");
        read("/api/items?lat=0&lng=0&keyword=" + tag + "&size=1", null)
                .andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.list[0].id").value(first));
        read("/api/items?lat=0&lng=0&keyword=" + tag + "&size=1&page=2", null)
                .andExpect(jsonPath("$.data.list[0].id").value(second));
        read("/api/items?lat=0&lng=0&keyword=" + tag + "&size=50&page=2147483647", null)
                .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test void invalidSearchInputsAreRejected() throws Exception {
        for (String query : List.of("lng=0", "lat=NaN&lng=0", "lat=Infinity&lng=0", "lat=91&lng=0", "lat=0&lng=181",
                "lat=0&lng=0&radius=99", "lat=0&lng=0&radius=10001", "lat=0&lng=0&category=bad", "lat=0&lng=0&status=OFFLINE",
                "lat=0&lng=0&status=", "lat=0&lng=0&page=0", "lat=0&lng=0&size=51")) {
            read("/api/items?" + query, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        }
    }

    @Test void reservationsDisappearFromAvailableSearchAndCancellationRestoresThem() throws Exception {
        Account owner = account(), borrower = account(); String tag = key().substring(0, 8);
        long item = publish(owner, tag, 0, 0, "SPORTS");
        long id = borrow(item, borrower);
        read("/api/items?lat=0&lng=0&keyword=" + tag, borrower).andExpect(jsonPath("$.data.total").value(0));
        action(id, "cancel", borrower).andExpect(status().isOk());
        read("/api/items?lat=0&lng=0&keyword=" + tag, borrower).andExpect(jsonPath("$.data.total").value(1));
    }
}
