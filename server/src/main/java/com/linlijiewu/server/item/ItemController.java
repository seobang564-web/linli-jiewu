package com.linlijiewu.server.item;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.linlijiewu.server.auth.AuthContext;
import com.linlijiewu.server.auth.RequireLogin;
import com.linlijiewu.server.common.ApiResponse;

@RestController
@RequestMapping("/api")
public class ItemController {
    private final ItemService items;
    public ItemController(ItemService items) { this.items = items; }
    @GetMapping("/items/{id}")
    public ApiResponse<ItemModels.Detail> detail(@PathVariable long id,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
            HttpServletRequest request) {
        return ApiResponse.success(items.detail(id, AuthContext.optional(request), lat, lng));
    }
    @RequireLogin
    @PostMapping("/items")
    public ApiResponse<ItemModels.Detail> create(@Valid @RequestBody ItemModels.Write body, HttpServletRequest request) {
        return ApiResponse.success(items.create(AuthContext.require(request), body));
    }
    @RequireLogin
    @PutMapping("/items/{id}")
    public ApiResponse<ItemModels.Detail> update(@PathVariable long id, @Valid @RequestBody ItemModels.Write body,
                                                HttpServletRequest request) {
        return ApiResponse.success(items.update(id, AuthContext.require(request), body));
    }
    @RequireLogin
    @PostMapping("/items/{id}/offline")
    public ApiResponse<ItemModels.Detail> offline(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.success(items.offline(id, AuthContext.require(request)));
    }
    @RequireLogin
    @GetMapping("/users/me/items")
    public ApiResponse<ItemModels.Page<ItemModels.Detail>> mine(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(items.mine(AuthContext.require(request), status, page, size));
    }
}
