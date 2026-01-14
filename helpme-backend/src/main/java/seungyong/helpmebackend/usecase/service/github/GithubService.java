package seungyong.helpmebackend.usecase.service.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.adapter.in.web.dto.repo.response.ResponseRepositories;
import seungyong.helpmebackend.usecase.port.in.github.GithubPortIn;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService implements GithubPortIn {
    @Override
    public ResponseRepositories getRepositories(Long userId) {
        return null;
    }
}
