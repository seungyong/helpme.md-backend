package seungyong.helpmebackend.infrastructure.jwt;

public interface JwtGenerator<T> {
    T generate(Long id);
}
