package com.linlijiewu.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import com.linlijiewu.server.common.BusinessException;

public final class AuthContext {
    public static final String USER_ID = AuthContext.class.getName() + ".userId";
    private AuthContext() { }
    public static Long optional(HttpServletRequest request) {
        return (Long) request.getAttribute(USER_ID);
    }
    public static long require(HttpServletRequest request) {
        Long id = optional(request);
        if (id == null) { throw BusinessException.unauthorized(); }
        return id;
    }
}
