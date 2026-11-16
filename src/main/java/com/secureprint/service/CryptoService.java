package com.secureprint.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private final SecretKey masterKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${app.master-key}") String encodedMasterKey) {
        byte[] key = Base64.getDecoder().decode(encodedMasterKey);
        if (key.length != 32) throw new IllegalArgumentException("APP_MASTER_KEY must decode to 32 bytes");
        this.masterKey = new SecretKeySpec(key, "AES");
    }

    public SecretKey generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256, random);
            return generator.generateKey();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to generate encryption key", ex);
        }
    }

    public byte[] wrapKey(SecretKey key) {
        return encrypt(key.getEncoded(), masterKey);
    }

    public SecretKey unwrapKey(byte[] wrappedKey) {
        return new SecretKeySpec(decrypt(wrappedKey, masterKey), "AES");
    }

    public byte[] encrypt(byte[] plaintext, SecretKey key) {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return concat(iv, cipher.doFinal(plaintext));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt data", ex);
        }
    }

    public byte[] decrypt(byte[] blob, SecretKey key) {
        if (blob.length <= IV_LENGTH) throw new IllegalArgumentException("Encrypted blob is invalid");
        byte[] iv = Arrays.copyOf(blob, IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(blob, IV_LENGTH, blob.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Encrypted data failed authentication", ex);
        }
    }

    private byte[] concat(byte[] left, byte[] right) {
        byte[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }
}
