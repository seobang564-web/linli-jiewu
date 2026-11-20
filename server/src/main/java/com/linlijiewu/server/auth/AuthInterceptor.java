package com.linlijiewu.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.linlijiewu.server.common.BusinessException;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwt;
    public AuthInterceptor(JwtService jwt) { this.jwt = jwt; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (!header.startsWith("Bearer ") || header.length() <= 7) {
                throw BusinessException.unauthorized();
            }
            request.setAttribute(AuthContext.USER_ID, jwt.verify(header.substring(7)));
        }
        if (handler instanceof HandlerMethod method
                && (method.hasMethodAnnotation(RequireLogin.class)
                    || method.getBeanType().isAnnotationPresent(RequireLogin.class))) {
            AuthContext.require(request);
        }
        return true;
    }
}
