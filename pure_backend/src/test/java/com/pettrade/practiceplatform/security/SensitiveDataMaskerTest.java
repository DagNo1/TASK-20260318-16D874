package com.pettrade.practiceplatform.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void masksPhoneAndIdAndGeneric() {
        assertEquals("****4567", masker.maskPhone("+1-555-123-4567"));
        assertEquals("****7788", masker.maskIdNumber("ABCD7788"));
        assertEquals("***REDACTED***", masker.maskGeneric("merchant_operator"));
    }
}
