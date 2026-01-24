package seungyong.helpmebackend.adapter.in.web.dto.installation.response;

import seungyong.helpmebackend.domain.entity.installation.Installation;

import java.util.List;

public record ResponseInstallations(
        List<Installation> installations
) {
}
