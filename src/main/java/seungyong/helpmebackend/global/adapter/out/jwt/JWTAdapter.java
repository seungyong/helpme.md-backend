package seungyong.helpmebackend.global.adapter.out.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JWTAdapter implements JWTPortOut {
    private final JWTProvider jwtProvider;

    @Override
    public JWT generate(JWTUser user) {
        return jwtProvider.generate(user);
    }

    @Override
    public boolean isExpired(String token, Date current) {
        return jwtProvider.isExpired(token, current);
    }
}
