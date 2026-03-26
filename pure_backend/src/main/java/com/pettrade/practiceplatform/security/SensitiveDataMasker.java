package com.pettrade.practiceplatform.security;

import org.springframework.stereotype.Component;

@Component
public class SensitiveDataMasker {

    public String maskPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    public String maskIdNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    public String maskGeneric(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "***REDACTED***";
    }
}
