package com.pettrade.practiceplatform.security;

import com.pettrade.practiceplatform.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SensitiveDataEncryptorTest {

    private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void encryptDecryptRoundTripWorks() {
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor(KEY);
        String ciphertext = encryptor.encrypt("15551234567");
        assertNotEquals("15551234567", ciphertext);
        assertEquals("15551234567", encryptor.decrypt(ciphertext));
    }

    @Test
    void tamperedCiphertextFailsVerification() {
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor(KEY);
        String ciphertext = encryptor.encrypt("ID-7788");
        String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "AA";
        assertThrows(BusinessRuleException.class, () -> encryptor.decrypt(tampered));
    }
}
