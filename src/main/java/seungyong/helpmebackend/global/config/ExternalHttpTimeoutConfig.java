package seungyong.helpmebackend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;

import java.time.Duration;

@Configuration
public class ExternalHttpTimeoutConfig {
    @Bean
    RestClientCustomizer externalHttpTimeouts(
            @Value("${external.http.connect-timeout:10s}") Duration connectTimeout,
            @Value("${external.http.read-timeout:120s}") Duration readTimeout
    ) {
        if (connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("External HTTP timeouts must be positive");
        }
        // 외부 시스템 장애로 삭제·내보내기 행 잠금이 무기한 유지되지 않도록 요청별 제한 적용
        return builder -> {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(connectTimeout).build());
            factory.setReadTimeout(readTimeout);
            builder.requestFactory(factory);
        };
    }
}
