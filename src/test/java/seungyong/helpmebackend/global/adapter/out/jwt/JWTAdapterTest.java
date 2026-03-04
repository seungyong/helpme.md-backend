package seungyong.helpmebackend.global.adapter.out.jwt;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.user.domain.entity.JWTUser;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JWTAdapterTest {
    @Mock private JWTProvider jwtProvider;
    @InjectMocks private JWTAdapter jwtAdapter;

    @Test
    @DisplayName("JWT 생성 - 성공")
    void generateJWT_Success() {
        JWTUser user = new JWTUser(1L, "testuser");
        JWT jwt = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .defaultNotNull(true)
                .build()
                .giveMeOne(JWT.class);

        Mockito
                .when(jwtProvider.generate(user))
                .thenReturn(jwt);

        JWT createdJwt = jwtAdapter.generate(user);

        assertNotNull(createdJwt);
    }

    @Nested
    class IsExpiredTests {
        @Test
        @DisplayName("JWT 만료 여부 확인 - 성공 (true)")
        void isExpired_success_true() {
            Mockito
                    .when(jwtProvider.isExpired(Mockito.anyString(), Mockito.any(Date.class)))
                    .thenReturn(true);

            boolean result = jwtAdapter.isExpired("test-token", new Date());
            assertTrue(result);
        }

        @Test
        @DisplayName("JWT 만료 여부 확인 - 성공 (false)")
        void isExpired_success_false() {
            Mockito
                    .when(jwtProvider.isExpired(Mockito.anyString(), Mockito.any(Date.class)))
                    .thenReturn(false);

            boolean result = jwtAdapter.isExpired("test-token", new Date());
            assertFalse(result);
        }
    }
}
