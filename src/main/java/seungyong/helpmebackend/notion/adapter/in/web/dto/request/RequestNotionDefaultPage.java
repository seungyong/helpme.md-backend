package seungyong.helpmebackend.notion.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestNotionDefaultPage(
        @NotBlank String defaultParentPageId,
        @NotBlank String defaultParentPageTitle
) {
}
