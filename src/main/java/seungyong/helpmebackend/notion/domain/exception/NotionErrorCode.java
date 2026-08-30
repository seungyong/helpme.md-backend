package seungyong.helpmebackend.notion.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum NotionErrorCode implements ErrorCode {
    NOTION_AUTHORIZATION_DENIED(
            HttpStatus.FORBIDDEN,
            "Notion 연결 권한이 거부되었습니다.",
            "NOTION_40301"
    ),
    NOTION_CONNECTION_EXPIRED(
            HttpStatus.FORBIDDEN,
            "Notion을 다시 연결해 주세요.",
            "NOTION_40302"
    ),
    NOTION_PAGE_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "선택한 Notion 페이지에 접근할 수 없습니다.",
            "NOTION_40303"
    ),
    NOTION_CONNECTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "연결된 Notion Workspace가 없습니다.",
            "NOTION_40401"
    ),
    NOTION_PARENT_PAGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "선택한 Notion 페이지를 찾을 수 없습니다.",
            "NOTION_40402"
    ),
    NOTION_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "Notion API 요청 한도를 초과했습니다.",
            "RATE_42901"
    ),
    NOTION_UPSTREAM_ERROR(
            HttpStatus.BAD_GATEWAY,
            "Notion API가 비정상 응답을 반환했습니다.",
            "NOTION_50201"
    );

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

    public static NotionErrorCode fromErrorCode(String errorCode) {
        if (errorCode == null) {
            return null;
        }
        for (NotionErrorCode value : values()) {
            if (value.errorCode.equals(errorCode)) {
                return value;
            }
        }
        return null;
    }
}
