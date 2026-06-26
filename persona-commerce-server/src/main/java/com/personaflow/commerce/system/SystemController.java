package com.personaflow.commerce.system;

import com.personaflow.commerce.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.success(
                Map.of(
                        "message", "pong",
                        "service", "persona-commerce-server"
                )
        );
    }
}