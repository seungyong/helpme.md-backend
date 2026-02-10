package seungyong.helpmebackend.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.user.JWTUser;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JWTProvider implements JwtGenerator<JWT> {
    private static final String grantType = "Bearer";
    private static final long accessTokenExpirationTime = 60 * 30;
    private static final long refreshTokenExpirationTime = 60 * 60 * 24 * 7;
    private final Key key;

    public JWTProvider(@Value("${jwt.secret}") String secretKey) {
        if (secretKey == null) {
            log.info("secretKey가 존재하지 않습니다.");
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 지원하지 않는 메서드
     *
     * @param id 사용자 ID
     * @return {@link JWT}
     */
    @Override
    @Deprecated
    public JWT generate(Long id) {
        throw new UnsupportedOperationException("Use generate(JWTUser user) instead.");
    }

    /**
     * JWTUser를 통해 JWT 생성
     *
     * @param user {@link JWTUser}
     * @return {@link JWT}
     */
    public JWT generate(JWTUser user) {
        Instant accessExpire = Instant.now().plusSeconds(accessTokenExpirationTime);
        Date accessTokenExpireDate = Date.from(accessExpire);

        Instant refreshExpire = Instant.now().plusSeconds(refreshTokenExpirationTime);
        Date refreshTokenExpireDate = Date.from(refreshExpire);

        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .setExpiration(accessTokenExpireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setExpiration(refreshTokenExpireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JWT.builder()
                .grantType(grantType)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpireTime(accessExpire)
                .refreshTokenExpireTime(refreshExpire)
                .build();
    }

    /**
     * AccessToken을 통해 JWTUser 생성
     *
     * @param accessToken AccessToken
     * @return {@link JWTUser}
     */
    public JWTUser getUserByToken(String accessToken) {
        Claims claims = parseClaims(accessToken, true);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        return new JWTUser(userId, username);
    }

    /**
     * AccessToken을 통해 JWTUser 생성 (만료된 토큰도 허용)
     *
     * @param accessToken AccessToken
     * @return {@link JWTUser}
     */
    public JWTUser getUserByTokenWithoutCheck(String accessToken){
        Claims claims = parseClaims(accessToken, false);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        return new JWTUser(userId, username);
    }

    /**
     * token 만료 여부 확인
     *
     * @param token AccessToken
     * @return 만료 여부
     */
    public Boolean isExpired(String token, Date current){
        Claims claims = parseClaims(token, false);
        return claims.getExpiration().before(current);
    }

    private Claims parseClaims(String token, boolean validate) {
        if (token == null) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        if (token.contains("Bearer")) {
            token = token.split(" ")[1].trim();
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            if (validate) {
                throw new CustomException(GlobalErrorCode.EXPIRED_ACCESS_TOKEN);
            }

            return e.getClaims();
        } catch (Exception e) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
    }
}
