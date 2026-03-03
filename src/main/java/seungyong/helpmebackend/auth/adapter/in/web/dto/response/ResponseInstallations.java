package seungyong.helpmebackend.auth.adapter.in.web.dto.response;

import seungyong.helpmebackend.auth.domain.entity.Installation;

import java.util.List;

public record ResponseInstallations(
        List<Installation> installations
) {
}
