package seungyong.helpmebackend.usecase.port.out.cipher;

public interface CipherPortOut {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
