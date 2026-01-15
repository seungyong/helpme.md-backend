package seungyong.helpmebackend.adapter.in.web.dto.installation.response;

import seungyong.helpmebackend.domain.entity.installation.Installation;

import java.util.ArrayList;

public record ResponseInstallations(
        ArrayList<Installation> installations
) {
}
