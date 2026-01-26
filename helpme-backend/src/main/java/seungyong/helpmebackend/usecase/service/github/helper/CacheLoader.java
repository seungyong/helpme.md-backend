package seungyong.helpmebackend.usecase.service.github.helper;

import com.fasterxml.jackson.core.JsonProcessingException;

@FunctionalInterface
public interface CacheLoader<T> {
    T load();
}
