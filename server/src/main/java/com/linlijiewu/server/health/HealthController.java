package com.linlijiewu.server.health;

import com.linlijiewu.server.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final HealthService service;

    public HealthController(HealthService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ApiResponse<HealthService.HealthData> health() {
        return ApiResponse.success(service.check());
    }
}
