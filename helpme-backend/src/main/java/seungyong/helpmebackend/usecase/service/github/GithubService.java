package seungyong.helpmebackend.usecase.service.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.adapter.in.web.dto.repo.response.ResponseRepositories;
import seungyong.helpmebackend.usecase.port.in.github.GithubPortIn;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService implements GithubPortIn {
    private final GithubPortOut githubPortOut;
    private final RedisPortOut redisPortOut;
    private final UserPortOut userPortOut;

    @Override
    public ResponseRepositories getRepositories(Long userId) {
        return null;
    }
}
