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
import seungyong.helpmebackend.infrastructure.mapper.CustomTimeStamp;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
     * UserId를 통해 AccessToken, RefreshToken 생성
     *
     * @param userId 사용자 ID
     * @return {@link JWT}
     */
    public JWT generate(Long userId) {
        CustomTimeStamp customTimestamp = new CustomTimeStamp();
        LocalDateTime accessTokenExpireTime = customTimestamp.getTimestamp()
                .plusSeconds(accessTokenExpirationTime);
        LocalDateTime refreshTokenExpireTime = customTimestamp.getTimestamp()
                .plusSeconds(refreshTokenExpirationTime);

        Date accessTokenExpireDate = Date.from(
                accessTokenExpireTime.atZone(ZoneId.systemDefault()).toInstant()
        );
        Date refreshTokenExpireDate = Date.from(
                refreshTokenExpireTime.atZone(ZoneId.systemDefault()).toInstant()
        );

        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(accessTokenExpireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setExpiration(refreshTokenExpireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JWT.builder()
                .grantType(grantType)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpireTime(accessTokenExpireTime)
                .refreshTokenExpireTime(refreshTokenExpireTime)
                .build();
    }

    /**
     * AccessToken을 통해 UserId를 반환
     *
     * @param accessToken AccessToken
     * @return UserId
     */
    public Long getUserIdByAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken, true);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * AccessToken을 통해 UserId를 반환 (만료 시간 체크 X)
     *
     * @param accessToken AccessToken
     * @return 만료 시간
     */
    public Long getUserIdByAccessTokenWithoutCheck(String accessToken){
        Claims claims = parseClaims(accessToken, false);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * token 만료 여부 확인
     *
     * @param token AccessToken
     * @return 만료 여부
     */
    public Boolean isExpired(String token){
        Claims claims = parseClaims(token, false);
        return claims.getExpiration().before(new Date());
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
