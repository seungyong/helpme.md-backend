package seungyong.helpmebackend.adapter.in.web.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.ErrorResponse;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.jwt.JWTProvider;

import java.io.IOException;

@Profile("!test")
@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private final JWTProvider jwtProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return (path.equals("/api/v1/oauth2/login") && method.equals("GET"))
                || (path.equals("/api/v1/oauth2/callback") && method.equals("GET"))
                || (path.equals("/api/v1/users/reissue") && method.equals("POST"))
                || path.endsWith(".html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || (path.equals("/api/v1/sse/subscribe") && method.equals("GET"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");

        String accessToken = extractToken(request, "accessToken");

        if (accessToken == null) {
            int status;
            String body;
            String refreshToken = extractToken(request, "refreshToken");

            if (refreshToken == null) {
                status = GlobalErrorCode.NOT_FOUND_TOKEN.getHttpStatus().value();
                body = ErrorResponse.toJson(GlobalErrorCode.NOT_FOUND_TOKEN);
            } else {
                status = GlobalErrorCode.EXPIRED_ACCESS_TOKEN.getHttpStatus().value();
                body = ErrorResponse.toJson(GlobalErrorCode.EXPIRED_ACCESS_TOKEN);
            }

            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(body);
            response.getWriter().flush();
            return;
        }

        try {
            Authentication authentication = getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (CustomException e) {
            response.setStatus(e.getErrorCode().getHttpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    ErrorResponse.toJson(e.getErrorCode())
            );
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        String accessToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }

        return accessToken;
    }

    private Authentication getAuthentication(String accessToken) {
        Long userId = jwtProvider.getUserIdByAccessToken(accessToken);

        CustomUserDetails customUser = new CustomUserDetails(
                userId
        );

        return new UsernamePasswordAuthenticationToken(customUser, null, customUser.getAuthorities());
    }
}
