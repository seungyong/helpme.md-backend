package seungyong.helpmebackend.user.adapter.out.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;

@Converter
public class EncryptedTokenConverter implements AttributeConverter<EncryptedToken, String> {
    @Override
    public String convertToDatabaseColumn(EncryptedToken attribute) {
        return attribute != null ? attribute.value() : null;
    }

    @Override
    public EncryptedToken convertToEntityAttribute(String dbData) {
        return dbData != null ? new EncryptedToken(dbData) : null;
    }
}
