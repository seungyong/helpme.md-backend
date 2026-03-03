package seungyong.helpmebackend.global.infrastructure.jwt;

public interface JwtGenerator<T> {
    T generate(Long id);
}
