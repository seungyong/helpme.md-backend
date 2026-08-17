package seungyong.helpmebackend.webhook;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.webhook.application.WebhookWorker;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "oauth2.github.apps.webhook-secret=test-webhook-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class WebhookIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @Autowired private WebhookWorker webhookWorker;
    private User savedUser;

    @AfterEach
    void cleanUp() {
        if (savedUser != null) {
            userPortOut.delete(savedUser);
        }
    }

    @Test
    void signedPushBecomesActivityAndRedeliveryIsNoOp() throws Exception {
        savedUser = userPortOut.save(new User(
                null,
                new GithubUser(
                        "octocat", 909090L,
                        new EncryptedToken(cipherPortOut.encrypt("raw-github-token"))
                )
        ));
        Project project = projectPortOut.save(Project.builder()
                .userId(savedUser.getId())
                .repoFullName("octocat/webhook-integration")
                .githubRepoId(778899L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .build());
        byte[] body = """
                {
                  "repository":{"id":778899,"full_name":"octocat/webhook-integration","private":false},
                  "installation":{"id":9001},
                  "sender":{"login":"octocat"},
                  "ref":"refs/heads/main",
                  "before":"0000",
                  "after":"abc123",
                  "commits":[{
                    "id":"abc123",
                    "message":"feat: collect webhook",
                    "timestamp":"2026-08-17T00:00:00Z",
                    "url":"https://github.com/octocat/webhook-integration/commit/abc123",
                    "distinct":true,
                    "author":{"username":"octocat"}
                  }]
                }
                """.getBytes(StandardCharsets.UTF_8);

        receive(body, "delivery-integration")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"));
        webhookWorker.runOnce();

        mockMvc.perform(get("/api/v1/projects/{projectId}/activities", project.getId())
                        .cookie(cookies(savedUser))
                        .param("from", "2026-08-17")
                        .param("to", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].commitSha").value("abc123"))
                .andExpect(jsonPath("$.items[0].title").value("feat: collect webhook"));

        receive(body, "delivery-integration")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("duplicate"));
    }

    private org.springframework.test.web.servlet.ResultActions receive(
            byte[] body, String deliveryId
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/webhooks/github")
                .header("X-Hub-Signature-256", sign(body))
                .header("X-GitHub-Event", "push")
                .header("X-GitHub-Delivery", deliveryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                "test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        ));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    private Cookie[] cookies(User user) {
        JWT jwt = jwtProvider.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));
        return new Cookie[]{
                new Cookie("accessToken", jwt.getAccessToken()),
                new Cookie("refreshToken", jwt.getRefreshToken())
        };
    }
}
