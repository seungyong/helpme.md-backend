package seungyong.helpmebackend.user.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.", "USER_40401", null),
    USER_DELETION_IN_PROGRESS(
            HttpStatus.CONFLICT,
            "회원 탈퇴 처리 중인 계정입니다.",
            "USER_40901",
            "sign_out"
    )
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
    private final String requiredAction;
}
