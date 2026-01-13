package seungyong.helpmebackend.adapter.out.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.usecase.port.out.jwt.JWTPortOut;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JWTAdapter implements JWTPortOut {
    private final JWTProvider jwtProvider;

    @Override
    public JWT generate(Long userId) {
        return jwtProvider.generate(userId);
    }

    @Override
    public boolean isExpired(String token, Date current) {
        return jwtProvider.isExpired(token, current);
    }

    @Override
    public Long getUserIdByAccessTokenWithoutCheck(String accessToken) {
        return jwtProvider.getUserIdByAccessTokenWithoutCheck(accessToken);
    }
}
