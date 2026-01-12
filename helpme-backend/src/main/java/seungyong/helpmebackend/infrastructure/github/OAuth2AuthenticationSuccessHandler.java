package seungyong.helpmebackend.infrastructure.github;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.jwt.JWTProvider;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JWTProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String githubId = oAuth2User.getAttribute("id").toString();

        // 회원가입 or 로그인
        Long userId = 1L;

        JWT jwt = jwtProvider.generate(userId);
        String redirectUrl = "http://localhost:3000/oauth2/redirect";

        getRedirectStrategy().sendRedirect(
                request,
                response,
                redirectUrl
        );
    }
}
