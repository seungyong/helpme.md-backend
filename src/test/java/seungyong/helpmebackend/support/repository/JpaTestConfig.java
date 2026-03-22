package seungyong.helpmebackend.support.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = {
        "seungyong.helpmebackend.*.adapter.out.persistence",
})
public class JpaTestConfig {
}
