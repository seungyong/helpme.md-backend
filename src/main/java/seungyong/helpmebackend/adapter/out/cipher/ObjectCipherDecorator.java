package seungyong.helpmebackend.adapter.out.cipher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.cipher.ObjectCipherPortOut;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectCipherDecorator implements ObjectCipherPortOut {
    private final CipherPortOut cipherPortOut;
    private final ObjectMapper objectMapper;


    @Override
    public <T> String encrypt(T data) {
        if (data == null) { return null; }

        try {
            String json = objectMapper.writeValueAsString(data);
            return cipherPortOut.encrypt(json);
        } catch (Exception e) {
            log.error("Object encryption error = {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public <T> T decrypt(String encryptedText, Class<T> type) {
        if (encryptedText == null) { return null; }

        try {
            String json = cipherPortOut.decrypt(encryptedText);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Object decryption error = {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public <T> T decrypt(String encryptedText, TypeReference<T> typeReference) {
        if (encryptedText == null) { return null; }

        try {
            String json = cipherPortOut.decrypt(encryptedText);
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("Object decryption error = {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
