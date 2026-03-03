package seungyong.helpmebackend.repository.domain.entity;

/**
 * 암호화된 토큰을 나타내는 클래스입니다.
 *
 * @param value 암호화된 토큰 값
 * @throws IllegalArgumentException 토큰 값이 null이거나 공백인 경우 발생
 */
public record EncryptedToken(String value) {
    public EncryptedToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("토큰 값은 null이거나 공백일 수 없습니다.");
        }
    }
}
