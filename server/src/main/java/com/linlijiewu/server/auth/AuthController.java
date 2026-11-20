package com.linlijiewu.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.linlijiewu.server.common.ApiResponse;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/auth/register")
    public ApiResponse<AuthModels.Token> register(@Valid @RequestBody AuthModels.Register body) {
        return ApiResponse.success(auth.register(body));
    }
    @PostMapping("/auth/login")
    public ApiResponse<AuthModels.Token> login(@Valid @RequestBody AuthModels.Login body) {
        return ApiResponse.success(auth.login(body));
    }
    @RequireLogin
    @GetMapping("/users/me")
    public ApiResponse<AuthModels.Me> me(HttpServletRequest request) {
        return ApiResponse.success(auth.me(AuthContext.require(request)));
    }
    @RequireLogin
    @PutMapping("/users/me")
    public ApiResponse<AuthModels.Me> update(@Valid @RequestBody AuthModels.Profile body, HttpServletRequest request) {
        return ApiResponse.success(auth.update(AuthContext.require(request), body));
    }
}
