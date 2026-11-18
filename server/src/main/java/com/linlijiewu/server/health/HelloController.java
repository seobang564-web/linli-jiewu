package com.linlijiewu.server.health;

import com.linlijiewu.server.common.ApiResponse;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/hello")
    public ApiResponse<HelloData> hello() {
        return ApiResponse.success(new HelloData("linli-jiewu", "Hello, 邻里借物地图！",
                OffsetDateTime.now(ZoneId.of("Asia/Shanghai"))));
    }

    public record HelloData(String service, String message, OffsetDateTime time) {
    }
}
