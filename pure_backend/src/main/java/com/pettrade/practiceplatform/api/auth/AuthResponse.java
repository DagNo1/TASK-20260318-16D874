package com.pettrade.practiceplatform.api.auth;

import java.util.List;

public record AuthResponse(String accessToken, String tokenType, String username, List<String> roles) {
}
