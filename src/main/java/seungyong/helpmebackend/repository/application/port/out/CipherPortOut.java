package seungyong.helpmebackend.repository.application.port.out;

public interface CipherPortOut {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
