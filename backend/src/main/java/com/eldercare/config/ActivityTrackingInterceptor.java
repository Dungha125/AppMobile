package com.eldercare.config;

import com.eldercare.security.CurrentUser;
import com.eldercare.service.PassiveCheckInTrackerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ActivityTrackingInterceptor implements HandlerInterceptor {

    private final PassiveCheckInTrackerService passiveCheckInTrackerService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || response.getStatus() >= 400) return;

        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) return;
        if (path.startsWith("/api/auth/")) return;

        String method = request.getMethod();
        if (method == null || method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("OPTIONS")) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser currentUser)) return;

        passiveCheckInTrackerService.recordUserActivity(currentUser.getUserId(), method.toUpperCase(), path);
    }
}
