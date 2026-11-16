package com.secureprint.service;

import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {
    private final CryptoService crypto = new CryptoService(Base64.getEncoder().encodeToString(new byte[32]));

    @Test void encryptsAndDecryptsAndUsesFreshIv() {
        SecretKey key = crypto.generateKey();
        byte[] plaintext = "%PDF-test".getBytes();
        byte[] first = crypto.encrypt(plaintext, key);
        byte[] second = crypto.encrypt(plaintext, key);
        assertArrayEquals(plaintext, crypto.decrypt(first, key));
        assertFalse(Arrays.equals(first, second));
    }

    @Test void tamperedCiphertextFailsAuthentication() {
        SecretKey key = crypto.generateKey();
        byte[] encrypted = crypto.encrypt("secret".getBytes(), key);
        encrypted[encrypted.length - 1] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> crypto.decrypt(encrypted, key));
    }

    @Test void wrappedKeyIsNotRawKey() {
        SecretKey key = crypto.generateKey();
        byte[] wrapped = crypto.wrapKey(key);
        assertFalse(Arrays.equals(key.getEncoded(), wrapped));
        assertArrayEquals(key.getEncoded(), crypto.unwrapKey(wrapped).getEncoded());
    }
}
