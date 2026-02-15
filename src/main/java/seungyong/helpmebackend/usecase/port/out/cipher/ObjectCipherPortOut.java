package seungyong.helpmebackend.usecase.port.out.cipher;

import com.fasterxml.jackson.core.type.TypeReference;

public interface ObjectCipherPortOut {
    <T> String encrypt(T data);
    <T> T decrypt(String encryptedText, Class<T> type);
    <T> T decrypt(String encryptedText, TypeReference<T> typeReference);
}
