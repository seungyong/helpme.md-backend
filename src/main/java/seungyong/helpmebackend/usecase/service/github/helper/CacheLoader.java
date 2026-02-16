package seungyong.helpmebackend.usecase.service.github.helper;

@FunctionalInterface
public interface CacheLoader<T> {
    T load();
}
